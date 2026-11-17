package br.com.archbase.controlplane.config;

import br.com.archbase.controlplane.access.ControlPlaneAccessInterceptor;
import br.com.archbase.controlplane.access.ControlPlaneExceptionHandler;
import br.com.archbase.controlplane.access.RequiresAdmin;
import br.com.archbase.controlplane.auth.ControlPlaneBearerAuthenticator;
import br.com.archbase.controlplane.token.ControlPlaneTokenValidator;
import br.com.archbase.controlplane.token.JwksKeyStore;
import br.com.archbase.controlplane.user.ControlPlaneUserResolver;
import br.com.archbase.controlplane.user.UserLinkStore;
import br.com.archbase.security.repository.UserJpaRepository;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Role;
import org.springframework.core.env.Environment;

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

    @Bean
    public ControlPlaneBearerAuthenticator controlPlaneBearerAuthenticator(ControlPlaneTokenValidator validator,
                                                                           ControlPlaneUserResolver userResolver,
                                                                           ControlPlaneProperties properties) {
        return new ControlPlaneBearerAuthenticator(validator, userResolver, properties);
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

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static Advisor controlPlaneAccessAdvisor() {
        ComposablePointcut pointcut = new ComposablePointcut(new AnnotationMatchingPointcut(RequiresAdmin.class, true))
                .union(new AnnotationMatchingPointcut(null, RequiresAdmin.class, true));
        DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(pointcut, new ControlPlaneAccessInterceptor());
        advisor.setOrder(0);
        return advisor;
    }
}
