/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.com.archbase.identifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuração para o módulo archbase-identifier.
 * <p>
 * Registra automaticamente o {@link IdentifierModule} no Jackson ObjectMapper
 * quando a propriedade {@code archbase.identifier.enabled} está {@code true}.
 * </p>
 * <p>
 * <b>Desabilitado por padrão</b> para manter compatibilidade com código existente.
 * </p>
 *
 * <p><b>Para ativar:</b></p>
 * <pre>
 * archbase:
 *   identifier:
 *     enabled: true
 * </pre>
 *
 * @see IdentifierModule
 * @see ArchbaseIdentifier
 */
@AutoConfiguration
@ConditionalOnClass(ObjectMapper.class)
@ConditionalOnProperty(prefix = "archbase.identifier", name = "enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(ArchbaseIdentifierProperties.class)
public class ArchbaseIdentifierAutoConfiguration {

    /**
     * Registra o módulo de identificador no ObjectMapper padrão.
     *
     * @param objectMapper o ObjectMapper do Spring
     * @return o ObjectMapper configurado
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper identifierObjectMapper(ObjectMapper objectMapper) {
        objectMapper.registerModule(new IdentifierModule());
        return objectMapper;
    }

    /**
     * Cria o módulo de identificador para uso manual.
     *
     * @return o módulo de identificador
     */
    @Bean
    @ConditionalOnMissingBean
    public IdentifierModule identifierModule() {
        return new IdentifierModule();
    }
}
