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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades de configuração para o módulo archbase-jackson.
 *
 * <p><b>Propriedades disponíveis:</b></p>
 * <pre>
 * archbase:
 *   jackson:
 *     enabled: false  # Desabilitado por padrão
 * </pre>
 */
@ConfigurationProperties(prefix = "archbase.jackson")
public class ArchbaseJacksonProperties {

    /**
     * Indica se o módulo Jackson está habilitado.
     * <p>
     * Quando {@code false}, o módulo não é registrado e o comportamento
     * padrão do Jackson é mantido.
     * </p>
     * <p>
     * <b>Padrão: false</b> para manter compatibilidade com código existente.
     * </p>
     */
    private boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
