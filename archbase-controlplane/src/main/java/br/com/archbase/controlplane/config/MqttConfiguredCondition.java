package br.com.archbase.controlplane.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Canal MQTT só com {@code archbase.controlplane.mqtt.uri} preenchida. {@code @ConditionalOnProperty} aceitaria a
 * chave presente e vazia.
 *
 * @since 3.4.6
 */
class MqttConfiguredCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String uri = context.getEnvironment().getProperty("archbase.controlplane.mqtt.uri");
        return uri != null && !uri.isBlank();
    }
}
