package br.com.archbase.controlplane.config;

import br.com.archbase.controlplane.ControlPlane;
import br.com.archbase.controlplane.access.ControlPlaneAccessInterceptor;
import br.com.archbase.controlplane.access.RequiresEntitlement;
import br.com.archbase.controlplane.access.RequiresSeat;
import br.com.archbase.controlplane.access.RestrictedModeInterceptor;
import br.com.archbase.controlplane.license.LicenseVerifier;
import br.com.archbase.controlplane.metrics.ControlPlaneGauges;
import br.com.archbase.controlplane.metrics.ControlPlaneMetrics;
import br.com.archbase.controlplane.state.ControlPlaneState;
import br.com.archbase.controlplane.state.ControlPlaneStateStore;
import br.com.archbase.controlplane.access.ControlPlaneExceptionHandler;
import br.com.archbase.controlplane.access.RequiresAdmin;
import br.com.archbase.controlplane.auth.ControlPlaneBearerAuthenticator;
import br.com.archbase.controlplane.command.CommandProcessor;
import br.com.archbase.controlplane.command.CommandVerifier;
import br.com.archbase.controlplane.heartbeat.HeartbeatService;
import br.com.archbase.controlplane.heartbeat.ManifestPublisher;
import br.com.archbase.controlplane.mqtt.ControlPlaneMqttClient;
import br.com.archbase.controlplane.provisioning.FeatureManifestProvider;
import br.com.archbase.controlplane.provisioning.TenantProvisioner;
import br.com.archbase.controlplane.token.ControlPlaneTokenValidator;
import br.com.archbase.controlplane.token.JwksKeyStore;
import br.com.archbase.controlplane.user.ControlPlaneUserResolver;
import br.com.archbase.controlplane.user.UserLinkStore;
import br.com.archbase.security.repository.UserJpaRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Role;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;
import java.time.Clock;

/**
 * Liga o Control Plane quando {@code archbase.controlplane.enabled=true}. Sem a chave, nenhum bean deste módulo é
 * criado e a aplicação se comporta exatamente como antes.
 *
 * @since 3.4.6
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "archbase.controlplane", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ControlPlaneProperties.class)
@Import(ControlPlaneAopRegistrar.class)
public class ControlPlaneAutoConfiguration {

    @Bean
    public ControlPlaneStartupValidator controlPlaneStartupValidator(ControlPlaneProperties properties,
                                                                     Environment environment) {
        return new ControlPlaneStartupValidator(properties, environment);
    }

    @Bean(initMethod = "initialize")
    public JwksKeyStore controlPlaneJwksKeyStore(ControlPlaneProperties properties,
                                                 ControlPlaneStartupValidator validated) {
        ControlPlaneProperties.Jwks jwks = properties.getJwks();
        return new JwksKeyStore(jwks.getUrl(), jwks.getLocalPath(), jwks.getRefreshMinInterval(), jwks.getTimeout(),
                Clock.systemUTC());
    }

    @Bean
    public ControlPlaneTokenValidator controlPlaneTokenValidator(JwksKeyStore keyStore,
                                                                 ControlPlaneProperties properties) {
        return new ControlPlaneTokenValidator(keyStore, properties.effectiveIssuer(), properties.getPublicUrl(),
                properties.getClockSkew(), properties.isSeatRequired());
    }

    @Bean
    public UserLinkStore controlPlaneUserLinkStore(DataSource dataSource) {
        return new UserLinkStore(dataSource);
    }

    @Bean
    public SmartInitializingSingleton controlPlaneSchemaInitializer(UserLinkStore links,
                                                                    ControlPlaneProperties properties) {
        return () -> {
            if (properties.getSchema().getMode() == ControlPlaneProperties.SchemaMode.APPLY) {
                links.createTableIfMissing();
            }
        };
    }

    @Bean
    public ControlPlaneUserResolver controlPlaneUserResolver(UserJpaRepository users, UserLinkStore links) {
        return new ControlPlaneUserResolver(users, links);
    }

    /** Relógio do módulo; um teste ou produto pode fornecer o próprio bean com este nome. */
    @Bean
    @ConditionalOnMissingBean(name = "controlPlaneClock")
    public Clock controlPlaneClock() {
        return Clock.systemUTC();
    }

    @Bean
    public ControlPlaneStateStore controlPlaneStateStore(DataSource dataSource) {
        return new ControlPlaneStateStore(dataSource);
    }

    @Bean
    public LicenseVerifier controlPlaneLicenseVerifier(JwksKeyStore keyStore, ControlPlaneProperties properties) {
        return new LicenseVerifier(keyStore, properties.effectiveIssuer(), properties.getInstanceId(),
                properties.getProduct());
    }

    @Bean(destroyMethod = "")
    public ControlPlaneState controlPlaneState(ControlPlaneStateStore store, LicenseVerifier verifier,
                                               ControlPlaneProperties properties,
                                               @Qualifier("controlPlaneClock") Clock clock) {
        return new ControlPlaneState(store, verifier, properties.getGracePeriod(), clock);
    }

    @Bean
    public ControlPlaneMetrics controlPlaneMetrics(ObjectProvider<MeterRegistry> registry) {
        return new ControlPlaneMetrics(registry.getIfAvailable(CompositeMeterRegistry::new));
    }

    /** Cria as tabelas (com {@code schema.mode=apply}), carrega o estado e liga a fachada {@code ControlPlane}. */
    @Bean
    public SmartInitializingSingleton controlPlaneStateInitializer(ControlPlaneStateStore store,
                                                                   ControlPlaneState state,
                                                                   ControlPlaneProperties properties,
                                                                   ControlPlaneMetrics metrics,
                                                                   JwksKeyStore keyStore) {
        return () -> {
            if (properties.getSchema().getMode() == ControlPlaneProperties.SchemaMode.APPLY) {
                store.createTablesIfMissing();
            }
            state.setMetrics(metrics);
            state.load(properties.getLicense().getPath());
            ControlPlaneGauges.bind(metrics.registry(), state, store, keyStore);
            ControlPlane.bind(state);
        };
    }

    @Bean
    public DisposableBean controlPlaneFacadeUnbinder(ControlPlaneState state) {
        return () -> ControlPlane.unbind(state);
    }

    @Bean
    public ControlPlaneBearerAuthenticator controlPlaneBearerAuthenticator(ControlPlaneTokenValidator validator,
                                                                           ControlPlaneUserResolver userResolver,
                                                                           ControlPlaneProperties properties,
                                                                           ControlPlaneState state) {
        return new ControlPlaneBearerAuthenticator(validator, userResolver, properties, state);
    }

    @Bean
    public WebMvcConfigurer controlPlaneRestrictedModeConfigurer(ControlPlaneState state,
                                                                 ControlPlaneProperties properties) {
        RestrictedModeInterceptor interceptor = new RestrictedModeInterceptor(state, properties.getRestrictedMode());
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(interceptor);
            }
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "archbase.controlplane", name = "legacy-jwt", havingValue = "reject")
    public br.com.archbase.controlplane.web.LocalLoginRoutesFilter controlPlaneLocalLoginRoutesFilter() {
        return new br.com.archbase.controlplane.web.LocalLoginRoutesFilter();
    }

    @Bean
    public ControlPlaneExceptionHandler controlPlaneExceptionHandler() {
        return new ControlPlaneExceptionHandler();
    }

    /**
     * Canal de comandos e eventos (Fase C). Só com {@code archbase.controlplane.mqtt.uri} preenchida.
     */
    @Configuration(proxyBeanMethods = false)
    @Conditional(MqttConfiguredCondition.class)
    static class MqttConfiguration {

        @Bean
        public CommandVerifier controlPlaneCommandVerifier(JwksKeyStore keyStore, ControlPlaneProperties properties) {
            return new CommandVerifier(keyStore, properties.effectiveIssuer(), properties.getInstanceId());
        }

        @Bean
        public CommandProcessor controlPlaneCommandProcessor(CommandVerifier verifier, ControlPlaneStateStore store,
                                                             ControlPlaneState state,
                                                             ObjectProvider<TenantProvisioner> provisioner,
                                                             ObjectProvider<PlatformTransactionManager> transactions,
                                                             DataSource dataSource) {
            PlatformTransactionManager manager = transactions.getIfUnique(
                    () -> new DataSourceTransactionManager(dataSource));
            return new CommandProcessor(verifier, store, state, provisioner, new TransactionTemplate(manager));
        }

        @Bean
        public ControlPlaneMqttClient controlPlaneMqttClient(ControlPlaneProperties properties,
                                                             CommandProcessor processor) {
            ControlPlaneProperties.Mqtt mqtt = properties.getMqtt();
            return new ControlPlaneMqttClient(mqtt.getUri(),
                    firstNonBlank(mqtt.getClientId(), properties.getInstanceId()),
                    firstNonBlank(mqtt.getUsername(), properties.getInstanceId()),
                    firstNonBlank(mqtt.getPassword(), properties.getInstanceCredential()),
                    properties.getInstanceId(), properties.getProduct(), productVersion(properties),
                    mqtt.getSessionExpiry(), mqtt.getReconnectMinDelay(), mqtt.getReconnectMaxDelay(),
                    mqtt.getKeepAlive(), processor);
        }

        @Bean
        public ManifestPublisher controlPlaneManifestPublisher(ControlPlaneMqttClient mqtt,
                                                               ControlPlaneStateStore store,
                                                               ObjectProvider<TenantProvisioner> provisioner,
                                                               ObjectProvider<FeatureManifestProvider> features,
                                                               ControlPlaneProperties properties) {
            ManifestPublisher publisher = new ManifestPublisher(mqtt, store, provisioner, features,
                    properties.getProduct(), productVersion(properties));
            mqtt.onConnect(reconnect -> publisher.publishIfChanged());
            return publisher;
        }

        @Bean
        public SmartInitializingSingleton controlPlaneMqttGauges(ControlPlaneMqttClient mqtt,
                                                                 ControlPlaneMetrics metrics) {
            return () -> ControlPlaneGauges.bindMqtt(metrics.registry(), mqtt);
        }

        @Bean
        public HeartbeatService controlPlaneHeartbeat(ControlPlaneMqttClient mqtt, ControlPlaneState state,
                                                      ControlPlaneStateStore store, ControlPlaneProperties properties) {
            String url = firstNonBlank(properties.getHeartbeat().getUrl(),
                    trimSlash(properties.getUrl()) + "/api/v1/instances/" + properties.getInstanceId() + "/heartbeat");
            return new HeartbeatService(mqtt, state, store, properties.getInstanceId(), properties.getProduct(),
                    productVersion(properties), url, properties.getInstanceCredential(),
                    properties.getHeartbeat().getInterval());
        }

        private static String productVersion(ControlPlaneProperties properties) {
            return firstNonBlank(properties.getProductVersion(), "desconhecida");
        }

        private static String firstNonBlank(String value, String fallback) {
            return value != null && !value.isBlank() ? value : fallback;
        }

        private static String trimSlash(String value) {
            return value != null && value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
        }
    }

    /**
     * {@code @RequiresAdmin}, {@code @RequiresSeat} e {@code @RequiresEntitlement}. O estado entra por
     * {@link ObjectProvider} porque o advisor é estático (infraestrutura de AOP) e é criado antes dos demais beans.
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static Advisor controlPlaneAccessAdvisor(ObjectProvider<ControlPlaneState> state) {
        ComposablePointcut pointcut = new ComposablePointcut(new AnnotationMatchingPointcut(RequiresAdmin.class, true))
                .union(new AnnotationMatchingPointcut(null, RequiresAdmin.class, true))
                .union(new AnnotationMatchingPointcut(RequiresSeat.class, true))
                .union(new AnnotationMatchingPointcut(null, RequiresSeat.class, true))
                .union(new AnnotationMatchingPointcut(RequiresEntitlement.class, true))
                .union(new AnnotationMatchingPointcut(null, RequiresEntitlement.class, true));
        MethodInterceptor lazy = invocation -> new ControlPlaneAccessInterceptor(state.getObject()).invoke(invocation);
        DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(pointcut, lazy);
        advisor.setOrder(0);
        return advisor;
    }
}
