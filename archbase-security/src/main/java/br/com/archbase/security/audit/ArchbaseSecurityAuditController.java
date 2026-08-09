package br.com.archbase.security.audit;

import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.SecurityEventJpaRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * A leitura da trilha.
 *
 * <p><b>Só administrador</b>, verificado aqui e explicitamente. Não depende de
 * {@code admin-endpoints.policy}, cujo padrão {@code permit} deixaria qualquer autenticado entrar —
 * e a trilha é o registro de quem mexeu em quê: nas mãos erradas, é o mapa de quem tem acesso a quê
 * e de quando cada proteção foi afrouxada.
 *
 * <p>Existe apenas com {@code archbase.security.audit.enabled=true}. Sem a trilha ligada não há o
 * que ler, e um endpoint que sempre devolve lista vazia induz a concluir que nada aconteceu.
 */
@RestController
@RequestMapping("/api/v1/security/audit")
@ConditionalOnProperty(name = "archbase.security.audit.enabled", havingValue = "true")
public class ArchbaseSecurityAuditController {

    /** Teto por página: a trilha é grande e uma consulta sem limite derruba a memória do servidor. */
    private static final int TAMANHO_MAXIMO = 200;

    private final SecurityEventJpaRepository eventRepository;

    public ArchbaseSecurityAuditController(SecurityEventJpaRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /**
     * Os acontecimentos, do mais recente para o mais antigo.
     *
     * <p>O período é obrigatório na prática — o padrão de trinta dias existe para que a primeira
     * consulta de quem abre a tela não varra a tabela inteira.
     */
    @GetMapping("/events")
    public ResponseEntity<Page<SecurityEventEntity>> eventos(
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) SecurityEventType tipo,
            @RequestParam(required = false) String inicio,
            @RequestParam(required = false) String fim,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        if (!ehAdministrador()) {
            return ResponseEntity.status(403).build();
        }

        LocalDateTime de = inicio == null ? LocalDateTime.now().minusDays(30) : LocalDateTime.parse(inicio);
        LocalDateTime ate = fim == null ? LocalDateTime.now() : LocalDateTime.parse(fim);

        Page<SecurityEventEntity> pagina = eventRepository.buscar(
                de, ate, tipo,
                // String vazia em vez de nulo: o filtro compara com LENGTH(:usuario) = 0, e um nulo
                // faria o PostgreSQL inferir bytea para o parâmetro — o mesmo erro de
                // "function lower(bytea) does not exist" que já apareceu na árvore.
                usuario == null ? "" : usuario,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), TAMANHO_MAXIMO)));

        return ResponseEntity.ok(pagina);
    }

    private boolean ehAdministrador() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserEntity user)) {
            return false;
        }
        return Boolean.TRUE.equals(user.getIsAdministrator()) && user.isEnabled();
    }
}
