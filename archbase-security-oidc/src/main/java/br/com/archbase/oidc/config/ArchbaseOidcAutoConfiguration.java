package br.com.archbase.oidc.config;

import br.com.archbase.oidc.auth.OidcBearerAuthenticator;
import br.com.archbase.oidc.exchange.OidcTokenExchangeController;
import br.com.archbase.oidc.provider.OidcProviderRegistry;
import br.com.archbase.oidc.user.ExternalIdentityStore;
import br.com.archbase.oidc.user.OidcIdentityLinker;
import br.com.archbase.oidc.user.OidcLocalCredentialPolicy;
import br.com.archbase.security.auth.external.ExternalBearerAuthenticator;
import br.com.archbase.security.repository.UserJpaRepository;
import br.com.archbase.security.schema.ArchbaseSecuritySchemaProperties;
import br.com.archbase.security.service.ArchbaseAuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;
import java.time.Clock;

/**
 * Liga o IdP externo quando o jar está no classpath <b>e</b> há ao menos um provedor configurado. Sem provedor,
 * nenhum bean é criado (ver {@link Inert}).
 *
 * @since 3.5.0
 */
@AutoConfiguration
@Conditional(OidcProvidersConfiguredCondition.class)
@EnableConfigurationProperties(ArchbaseOidcProperties.class)
public class ArchbaseOidcAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ArchbaseOidcAutoConfiguration.class);

    @Bean
    public OidcProviderRegistry oidcProviderRegistry(ArchbaseOidcProperties properties) {
        OidcStartupValidator.validate(properties);
        log.info("oidc.providers.enabled ids={}", properties.getProviders().keySet());
        return new OidcProviderRegistry(properties, Clock.systemUTC());
    }

    @Bean
    public ExternalIdentityStore oidcExternalIdentityStore(DataSource dataSource) {
        return new ExternalIdentityStore(dataSource);
    }

    /** Segue {@code archbase.security.schema.mode}, como as tabelas do próprio módulo de segurança. */
    @Bean
    public SmartInitializingSingleton oidcSchemaInitializer(ExternalIdentityStore store,
                                                            ObjectProvider<ArchbaseSecuritySchemaProperties> schema) {
        return () -> {
            ArchbaseSecuritySchemaProperties.Mode mode = schema.getIfAvailable(ArchbaseSecuritySchemaProperties::new)
                    .getMode();
            if (mode == ArchbaseSecuritySchemaProperties.Mode.OFF || store.tableExists()) {
                return;
            }
            if (mode == ArchbaseSecuritySchemaProperties.Mode.REPORT) {
                log.warn("oidc.schema.missing table={} — crie com:\n{};\n{};", ExternalIdentityStore.TABLE,
                        ExternalIdentityStore.DDL, ExternalIdentityStore.INDEX_DDL);
                return;
            }
            store.createTableIfMissing();
        };
    }

    @Bean
    public OidcIdentityLinker oidcIdentityLinker(UserJpaRepository users, ExternalIdentityStore store,
                                                 ArchbaseOidcProperties properties) {
        return new OidcIdentityLinker(users, store, properties);
    }

    @Bean
    @Order(ExternalBearerAuthenticator.ORDER_OIDC)
    public OidcBearerAuthenticator oidcBearerAuthenticator(OidcProviderRegistry registry, OidcIdentityLinker linker) {
        return new OidcBearerAuthenticator(registry, linker);
    }

    @Bean
    public OidcLocalCredentialPolicy oidcLocalCredentialPolicy(ExternalIdentityStore store,
                                                               ArchbaseOidcProperties properties) {
        return new OidcLocalCredentialPolicy(store, properties);
    }

    /** Troca de token por sessão local — só com {@code archbase.security.oidc.exchange.enabled=true}. */
    @Bean
    @ConditionalOnProperty(prefix = "archbase.security.oidc.exchange", name = "enabled", havingValue = "true")
    public OidcTokenExchangeController oidcTokenExchangeController(OidcBearerAuthenticator authenticator,
                                                                   ArchbaseAuthenticationService authenticationService,
                                                                   ArchbaseOidcProperties properties) {
        log.info("oidc.exchange.enabled path=/api/v1/auth/oidc/exchange ttl={}",
                properties.getExchange().getLocalTokenTtl());
        return new OidcTokenExchangeController(authenticator, authenticationService,
                properties.getExchange().getLocalTokenTtl());
    }

    /** Jar presente, nenhum provedor: avisa e não cria nada. */
    @AutoConfiguration
    @Conditional(OidcProvidersConfiguredCondition.Not.class)
    public static class Inert {
        @Bean
        public SmartInitializingSingleton oidcInertWarning() {
            return () -> log.warn("archbase-security-oidc está no classpath, mas nenhum provedor foi configurado em "
                    + "archbase.security.oidc.providers — o módulo está inerte.");
        }
    }
}
