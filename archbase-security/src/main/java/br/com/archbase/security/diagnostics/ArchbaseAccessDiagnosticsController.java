package br.com.archbase.security.diagnostics;

import br.com.archbase.security.access.AccessDecision;
import br.com.archbase.security.access.AccessRequirement;
import br.com.archbase.security.persistence.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Diagnóstico de autorização: panorama, efetivo de um usuário e simulação.
 *
 * <p><b>Desligado por padrão.</b> Sobe apenas com
 * {@code archbase.security.diagnostics.enabled=true}. E, mesmo ligado, <b>exige
 * {@code isAdministrator}</b> — verificado aqui, explicitamente, sem depender de
 * {@code archbase.security.admin-endpoints.policy}, cujo padrão {@code permit} deixaria qualquer
 * autenticado entrar.
 *
 * <p>A razão da dupla trava: estes endpoints revelam a estrutura de acesso do tenant. Simulação é
 * ferramenta de diagnóstico, não de reconhecimento — quem consegue perguntar "o usuário X consegue
 * fazer Y?" para qualquer X e qualquer Y tem um mapa do que atacar. A resposta nunca inclui dado
 * pessoal além do identificador consultado.
 */
@RestController
@RequestMapping("/api/v1/security/diagnostics")
@ConditionalOnProperty(name = "archbase.security.diagnostics.enabled", havingValue = "true")
public class ArchbaseAccessDiagnosticsController {

    private static final Logger log = LoggerFactory.getLogger(ArchbaseAccessDiagnosticsController.class);

    private final ArchbaseAccessDiagnosticsService diagnostics;

    public ArchbaseAccessDiagnosticsController(ArchbaseAccessDiagnosticsService diagnostics) {
        this.diagnostics = diagnostics;
    }

    /** Os números do tenant corrente, com o estado das proteções junto. */
    @GetMapping("/overview")
    public ResponseEntity<AccessOverview> overview() {
        if (!ehAdministrador()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(diagnostics.overview());
    }

    /**
     * Os itens por trás de um número do panorama.
     *
     * <p>Um card diz "29 ações inativas"; sozinho, isso não permite agir. Aqui vem <b>quais</b> são,
     * com o motivo de cada uma estar na lista — o que transforma o painel de leitura em ponto de
     * partida de trabalho.
     *
     * <p>Paginado porque o catálogo de um tenant grande não cabe numa resposta só. A métrica é um
     * enum: o que não estiver nele responde 400, em vez de virar filtro arbitrário sobre o catálogo.
     */
    @GetMapping("/overview/{metric}")
    public ResponseEntity<Page<OverviewItem>> overviewItems(
            @PathVariable OverviewMetric metric,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        if (!ehAdministrador()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        // Teto no tamanho: sem ele, ?size=1000000 vira um pedido de tabela inteira em memória.
        int tamanho = Math.min(Math.max(size, 1), 200);
        return ResponseEntity.ok(diagnostics.listOverviewItems(metric, PageRequest.of(Math.max(page, 0), tamanho)));
    }

    /**
     * Um ramo da árvore de objetos de segurança.
     *
     * <p>Pessoas, grupos, perfis, recursos e as ações de um recurso — cada ramo buscado ao abrir, e
     * não a árvore inteira: um tenant real tem 621 ações e 104 recursos, e mandar tudo numa carga
     * só trava o navegador e obriga o servidor a materializar o catálogo para exibir cinco linhas.
     *
     * <p>O filtro roda no banco. Buscar no cliente exigiria ter carregado tudo antes — exatamente o
     * que esta paginação existe para evitar.
     */
    @GetMapping("/tree/{branch}")
    public ResponseEntity<Page<TreeNode>> browse(
            @PathVariable TreeBranch branch,
            @RequestParam(required = false) String parentId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        if (!ehAdministrador()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        int tamanho = Math.min(Math.max(size, 1), 200);
        try {
            return ResponseEntity.ok(
                    diagnostics.browse(branch, parentId, q, PageRequest.of(Math.max(page, 0), tamanho)));
        } catch (IllegalArgumentException e) {
            // Ramo de ações sem o recurso pai: pedido malformado, não erro de servidor.
            return ResponseEntity.badRequest().build();
        }
    }

    /** Quem está no grupo e o que cada pessoa acaba tendo, somando todas as origens. */
    @GetMapping("/groups/{groupId}")
    public ResponseEntity<GroupReport> group(@PathVariable String groupId) {
        if (!ehAdministrador()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return diagnostics.group(groupId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Idem para perfil — a via mais ampla, que vale para todo mundo que o tem. */
    @GetMapping("/profiles/{profileId}")
    public ResponseEntity<GroupReport> profile(@PathVariable String profileId) {
        if (!ehAdministrador()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return diagnostics.profile(profileId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Quem alcança uma capacidade — a consulta reversa.
     *
     * <p>"Quem consegue aprovar custo?" hoje só se responde abrindo grupo por grupo no admin, e a
     * resposta depende de quem cruzou. Inclui administrador, com a via marcada: ele alcança sem
     * concessão nenhuma, e omiti-lo daria a resposta errada.
     */
    @GetMapping("/actions/{actionId}/reach")
    public ResponseEntity<java.util.List<ReachEntry>> reach(@PathVariable String actionId) {
        if (!ehAdministrador()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        // Consultar quem alcança o quê é ação de auditoria: fica registrada, com quem perguntou.
        log.info("Consulta reversa da ação {} solicitada por {}",
                actionId, usuarioLogado() == null ? "desconhecido" : usuarioLogado().getId());
        return ResponseEntity.ok(diagnostics.whoCanReach(actionId));
    }

    /** O que a pessoa pode, achatado, com origem e situação por linha. */
    @GetMapping("/users/{userId}/effective")
    public ResponseEntity<EffectiveAccessReport> effective(@PathVariable String userId) {
        if (!ehAdministrador()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return diagnostics.effective(userId, null)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Idem, por e-mail — o identificador que quem opera o admin tem em mãos. */
    @GetMapping("/effective")
    public ResponseEntity<EffectiveAccessReport> effectiveByEmail(@RequestParam String email) {
        if (!ehAdministrador()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return diagnostics.effective(null, email)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * "Esta pessoa conseguiria fazer isto?"
     *
     * <p>Avalia sem executar nada, pelo mesmo avaliador que decide em produção. A resposta diz em
     * que portão parou e por quê.
     */
    @PostMapping("/simulate")
    public ResponseEntity<AccessDecision> simulate(@RequestBody SimulationRequest pedido) {
        if (!ehAdministrador()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (pedido == null
                || (isBlank(pedido.userId()) && isBlank(pedido.email()))
                || isBlank(pedido.resource()) || isBlank(pedido.action())) {
            return ResponseEntity.badRequest().build();
        }

        AccessRequirement requisito = AccessRequirement.of(
                pedido.resource(), pedido.action(),
                pedido.tenantId(), pedido.companyId(), pedido.projectId());

        // Consultar o acesso de outra pessoa é ação sensível: fica registrada, com quem perguntou.
        log.info("Simulação de acesso a {}:{} para {} solicitada por {}",
                pedido.resource(), pedido.action(),
                isBlank(pedido.userId()) ? pedido.email() : pedido.userId(),
                usuarioLogado() == null ? "desconhecido" : usuarioLogado().getId());

        return diagnostics.simulate(pedido.userId(), pedido.email(), requisito)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private boolean ehAdministrador() {
        UserEntity user = usuarioLogado();
        return user != null && Boolean.TRUE.equals(user.getIsAdministrator()) && user.isEnabled();
    }

    private UserEntity usuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserEntity user)) {
            return null;
        }
        return user;
    }

    private static boolean isBlank(String valor) {
        return valor == null || valor.isBlank();
    }
}
