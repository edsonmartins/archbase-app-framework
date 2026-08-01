package br.com.archbase.multitenancy.async;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Leva o contexto da requisição para a thread que executa a tarefa assíncrona.
 *
 * <p>Copia três coisas, capturadas no momento da <b>submissão</b> (ainda na thread da requisição) e
 * instaladas na thread do pool:
 *
 * <ul>
 *   <li><b>tenant</b> — já era copiado;</li>
 *   <li><b>company</b> — não era. Uma tarefa assíncrona rodava com o tenant certo e sem company,
 *       então o {@code companyFilter} do Hibernate não era ativado e a consulta enxergava todas as
 *       companies do tenant;</li>
 *   <li><b>SecurityContext</b> — não era. Sem ele o usuário autenticado some dentro do
 *       {@code @Async}: auditoria grava {@code createdByUser} vazio e qualquer checagem de permissão
 *       feita ali dentro nega. Desligável com
 *       {@code archbase.multitenancy.async.propagate-security-context=false}.</li>
 * </ul>
 *
 * <p>O estado anterior da thread é <b>restaurado</b> no fim, não zerado: a thread vem de um pool e
 * pode já estar carregando contexto de outro trabalho. A versão anterior chamava
 * {@code setTenantId(null)}, que além de não restaurar nada deixava a entrada do ThreadLocal viva
 * com valor nulo em vez de removê-la.
 */
public class ArchbaseTenantAwareTaskDecorator implements TaskDecorator {

    private final boolean propagateSecurityContext;

    public ArchbaseTenantAwareTaskDecorator() {
        this(true);
    }

    public ArchbaseTenantAwareTaskDecorator(boolean propagateSecurityContext) {
        this.propagateSecurityContext = propagateSecurityContext;
    }

    @Override
    @NonNull
    public Runnable decorate(@NonNull Runnable runnable) {
        // Capturado na thread que submete — é lá que o contexto da requisição existe.
        String tenantId = ArchbaseTenantContext.getTenantId();
        String companyId = ArchbaseTenantContext.getCompanyId();
        SecurityContext securityContext = propagateSecurityContext
                ? SecurityContextHolder.getContext()
                : null;

        return () -> {
            String previousTenantId = ArchbaseTenantContext.getTenantId();
            String previousCompanyId = ArchbaseTenantContext.getCompanyId();
            SecurityContext previousSecurityContext = SecurityContextHolder.getContext();

            try {
                ArchbaseTenantContext.setTenantId(tenantId);
                ArchbaseTenantContext.setCompanyId(companyId);
                if (securityContext != null) {
                    SecurityContextHolder.setContext(securityContext);
                }
                runnable.run();
            } finally {
                restore(previousTenantId, previousCompanyId);
                if (securityContext != null) {
                    SecurityContextHolder.setContext(previousSecurityContext);
                }
            }
        };
    }

    private void restore(String previousTenantId, String previousCompanyId) {
        // clear() primeiro, sempre: setTenantId(null) deixaria a entrada do ThreadLocal viva com
        // valor nulo — exatamente o que esta classe corrige. Só o que existia antes é reposto.
        ArchbaseTenantContext.clear();
        if (previousTenantId != null) {
            ArchbaseTenantContext.setTenantId(previousTenantId);
        }
        if (previousCompanyId != null) {
            ArchbaseTenantContext.setCompanyId(previousCompanyId);
        }
    }
}
