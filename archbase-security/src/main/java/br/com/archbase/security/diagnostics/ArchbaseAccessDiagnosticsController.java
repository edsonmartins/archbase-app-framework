package br.com.archbase.security.diagnostics;

import br.com.archbase.security.access.AccessDecision;
import br.com.archbase.security.access.AccessRequirement;
import br.com.archbase.security.persistence.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
