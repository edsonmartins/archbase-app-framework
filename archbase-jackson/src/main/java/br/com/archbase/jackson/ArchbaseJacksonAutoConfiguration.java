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
package br.com.archbase.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Auto-configuração para o módulo archbase-jackson.
 * <p>
 * Registra automaticamente o {@link ArchbaseJacksonModule} no Jackson ObjectMapper
 * quando a propriedade {@code archbase.jackson.enabled} está {@code true}.
 * </p>
 * <p>
 * <b>Desabilitado por padrão</b> para manter compatibilidade com código existente.
 * </p>
 *
 * <p><b>Para ativar:</b></p>
 * <pre>
 * archbase:
 *   jackson:
 *     enabled: true
 * </pre>
 *
 * @see ArchbaseJacksonModule
 */
@AutoConfiguration
@ConditionalOnClass(ObjectMapper.class)
@ConditionalOnProperty(prefix = "archbase.jackson", name = "enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(ArchbaseJacksonProperties.class)
public class ArchbaseJacksonAutoConfiguration {

    /**
     * Registra o módulo Archbase Jackson no ObjectMapper padrão.
     *
     * @param objectMapper o ObjectMapper do Spring
     * @return o ObjectMapper configurado
     */
    @org.springframework.context.annotation.Bean
    @ConditionalOnMissingBean
    public ObjectMapper jacksonObjectMapper(ObjectMapper objectMapper) {
        objectMapper.registerModule(new ArchbaseJacksonModule());
        return objectMapper;
    }

    /**
     * Cria o módulo Archbase Jackson para uso manual.
     *
     * @return o módulo Archbase Jackson
     */
    @org.springframework.context.annotation.Bean
    @ConditionalOnMissingBean
    public ArchbaseJacksonModule archbaseJacksonModule() {
        return new ArchbaseJacksonModule();
    }
}
