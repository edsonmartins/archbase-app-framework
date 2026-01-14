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

import java.io.Serializable;

/**
 * Interface marcadora para identificadores de entidades.
 * <p>
 * Tipos que implementam esta interface são reconhecidos como identificadores
 * pelo framework Archbase, permitindo tratamento especial em serialização,
 * validação e arquitetura.
 * </p>
 *
 * <p><b>Exemplo de uso:</b></p>
 * <pre>{@code
 * public class ClienteId implements Identifier {
 *
 *     private final UUID value;
 *
 *     public ClienteId(UUID value) {
 *         this.value = Objects.requireNonNull(value);
 *     }
 *
 *     public UUID getValue() {
 *         return value;
 *     }
 * }
 * }</pre>
 *
 * @param <T> o tipo do valor do identificador
 * @see ArchbaseIdentifier
 */
public interface Identifier<T> extends Serializable {

    /**
     * Retorna o valor do identificador.
     *
     * @return o valor do identificador
     */
    T getValue();
}
