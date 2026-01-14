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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação para marcar campos que representam identificadores de entidades.
 * <p>
 * Quando usada em conjunto com o módulo archbase-identifier, permite
 * serialização personalizada de identificadores via Jackson.
 * </p>
 * <p>
 * Esta anotação é opcional e não afeta o comportamento padrão das entidades.
 * Seu uso é puramente declarativo e serve para:
 * </p>
 * <ul>
 *   <li>Documentar explicitamente quais campos são identificadores</li>
 *   <li>Habilitar serialização otimizada via Jackson quando o módulo está ativo</li>
 *   <li>Facilitar validações de arquitetura com ArchUnit</li>
 * </ul>
 *
 * <p><b>Exemplo de uso:</b></p>
 * <pre>{@code
 * @Entity
 * public class Cliente extends PersistenceEntityBase<Cliente, UUID> {
 *
 *     @ArchbaseIdentifier
 *     private UUID id;
 *
 *     private String nome;
 * }
 * }</pre>
 *
 * @see Identifier
 */
@Target({ElementType.FIELD, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ArchbaseIdentifier {

    /**
     * Descrição opcional do identificador.
     * <p>
     * Pode ser usado para documentar o propósito do identificador
     * em contextos de arquitetura ou validação.
     * </p>
     *
     * @return descrição do identificador
     */
    String value() default "";
}
