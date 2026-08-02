package br.com.archbase.security.config;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.security.access.AccessDecision;
import br.com.archbase.security.access.AccessRequirement;
import br.com.archbase.security.annotation.HasPermission;
import br.com.archbase.security.service.ArchbaseSecurityService;
import br.com.archbase.security.util.AuthorizationAnnotationUtils;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class CustomAuthorizationManager implements AuthorizationManager<MethodInvocation> {

    private static final Logger log = LoggerFactory.getLogger(CustomAuthorizationManager.class);

    private final ArchbaseSecurityService securityService;
    
    public CustomAuthorizationManager(ArchbaseSecurityService securityService) {
        this.securityService = securityService;
    }

    @Override
    public AuthorizationDecision authorize(Supplier<? extends Authentication> authentication, MethodInvocation methodInvocation) {
        HasPermission hasPermission = AuthorizationAnnotationUtils.findAnnotation(methodInvocation, HasPermission.class);

        if (hasPermission == null) {
            // O interceptador só roda quando o pointcut casou, ou seja: a anotação existe em algum
            // lugar e a resolução é que falhou. Liberar aqui transformaria uma falha de leitura em
            // acesso concedido — nega, e deixa rastro para a investigação.
            log.error("Interceptação de @HasPermission sem anotação resolvível em {}#{} — acesso negado",
                    methodInvocation.getMethod().getDeclaringClass().getName(),
                    methodInvocation.getMethod().getName());
            return new AuthorizationDecision(false);
        }

        try {
            String tenantId = hasPermission.tenantId().isEmpty() ?
                ArchbaseTenantContext.getTenantId() : hasPermission.tenantId();
            String companyId = hasPermission.companyId().isEmpty() ?
                ArchbaseTenantContext.getCompanyId() : hasPermission.companyId();

            String origem = AuthorizationAdapters.origin(methodInvocation);
            AccessDecision decisao = securityService.decide(
                    authentication.get(),
                    AccessRequirement.of(
                            hasPermission.resource(),
                            hasPermission.action(),
                            tenantId,
                            companyId,
                            hasPermission.projectId()).withOrigin(origem));

            AuthorizationAdapters.log(log, decisao, origem);
            return new AuthorizationDecision(decisao.allowed());

        } catch (Exception e) {
            // Falha ao AVALIAR a permissão não é o mesmo que "não tem permissão", mas negar é a
            // opção segura. O que não pode é negar em silêncio: sem este log, um erro de consulta ou
            // um principal inesperado viram um 403 idêntico ao de falta de permissão, e a
            // investigação vai parar no cadastro de permissões — que está correto.
            log.error("Erro ao avaliar permissão action={} resource={}: {}",
                    hasPermission.action(), hasPermission.resource(), e.getMessage(), e);
            return new AuthorizationDecision(false);
        }
    }
}