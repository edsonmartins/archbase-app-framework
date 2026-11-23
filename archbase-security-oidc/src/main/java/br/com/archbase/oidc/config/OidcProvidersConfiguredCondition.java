package br.com.archbase.oidc.config;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Verdadeira quando há ao menos um provedor em {@code archbase.security.oidc.providers}.
 *
 * @since 3.5.0
 */
public class OidcProvidersConfiguredCondition implements Condition {

    static final String PREFIX = "archbase.security.oidc.providers";

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return configured(context);
    }

    static boolean configured(ConditionContext context) {
        return Binder.get(context.getEnvironment())
                .bind(PREFIX, Bindable.mapOf(String.class, Object.class))
                .map(m -> !m.isEmpty())
                .orElse(false);
    }

    /** O inverso, para o aviso de módulo inerte. */
    public static class Not implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return !configured(context);
        }
    }
}
