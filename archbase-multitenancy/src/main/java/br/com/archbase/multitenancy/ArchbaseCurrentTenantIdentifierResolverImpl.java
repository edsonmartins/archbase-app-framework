package br.com.archbase.multitenancy;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.util.Map;

@Component
public class ArchbaseCurrentTenantIdentifierResolverImpl implements CurrentTenantIdentifierResolver, HibernatePropertiesCustomizer {

    private static final Logger logger = LoggerFactory.getLogger(ArchbaseCurrentTenantIdentifierResolverImpl.class);

    /** Último recurso histórico quando nem contexto nem propriedade dizem qual é o tenant. */
    private static final String LEGACY_FALLBACK_TENANT = "archbase";

    private final TenantProperties tenantProperties;

    /**
     * Recusa a operação quando não há tenant no contexto, em vez de cair no tenant padrão.
     *
     * <p>O comportamento padrão é <b>abrir</b>: sem tenant resolvido, toda consulta passa a ler e
     * gravar no tenant default — dados de um tenant respondendo por outro, sem erro nenhum. Isso é
     * conveniente para tarefas de sistema (migrations, jobs de bootstrap) e perigoso para requisição
     * de usuário. Ligue em aplicação onde todo acesso legítimo sempre carrega tenant:
     *
     * <pre>archbase.app.tenant.fail-on-missing=true</pre>
     */
    @Value("${archbase.app.tenant.fail-on-missing:false}")
    private boolean failOnMissingTenant;

    public ArchbaseCurrentTenantIdentifierResolverImpl(TenantProperties tenantProperties) {
        this.tenantProperties = tenantProperties;
    }

    /**
     * Sem {@code setTenantId} aqui, de propósito. A versão anterior gravava o tenant padrão no
     * ThreadLocal durante a inicialização do bean — ou seja, na thread que sobe a aplicação, que
     * depois volta para o pool carregando esse valor. Era redundante
     * ({@link #resolveCurrentTenantIdentifier()} já cai no padrão quando o contexto está vazio) e
     * poluía uma thread que serviria requisições.
     */
    @PostConstruct
    public void init() {
        logger.info("Tenant padrão configurado: {} (fail-on-missing={})",
                tenantProperties.getId(), failOnMissingTenant);
    }

    @Override
    public String resolveCurrentTenantIdentifier() {
        String contextTenantId = ArchbaseTenantContext.getTenantId();
        if (!ObjectUtils.isEmpty(contextTenantId)) {
            return contextTenantId;
        }
        return resolveFallbackTenantId();
    }

    private String resolveFallbackTenantId() {
        if (failOnMissingTenant) {
            throw new IllegalStateException(
                    "Nenhum tenant no contexto e archbase.app.tenant.fail-on-missing=true. "
                            + "A operação foi recusada em vez de recair no tenant padrão.");
        }

        String tenantId = tenantProperties.getId();
        if (ObjectUtils.isEmpty(tenantId)) {
            // Nada configurado: o valor fixo abaixo é o comportamento histórico, mantido para não
            // quebrar quem depende dele — mas é adivinhação, e precisa aparecer no log como tal.
            logger.warn("Nenhum tenant no contexto e archbase.app.tenant.default.id não configurado; "
                    + "usando '{}'. Configure o tenant padrão ou "
                    + "archbase.app.tenant.fail-on-missing=true.", LEGACY_FALLBACK_TENANT);
            return LEGACY_FALLBACK_TENANT;
        }

        logger.debug("Nenhum tenant no contexto; usando o padrão configurado: {}", tenantId);
        return tenantId;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put("hibernate.tenant_identifier_resolver", this);
    }
}
