package br.com.archbase.controlplane.config;

import org.springframework.aop.config.AopConfigUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Garante um auto-proxy creator para os advisors de infraestrutura do módulo ({@code @RequiresAdmin}). Sem isso,
 * numa aplicação com {@code archbase.security.method.enabled=false} a anotação ficaria inerte em silêncio.
 * {@link AopConfigUtils#registerAutoProxyCreatorIfNecessary} não substitui um creator que já exista.
 *
 * @since 3.4.6
 */
class ControlPlaneAopRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        AopConfigUtils.registerAutoProxyCreatorIfNecessary(registry);
    }
}
