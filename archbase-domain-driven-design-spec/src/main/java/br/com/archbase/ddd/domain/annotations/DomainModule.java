/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.com.archbase.ddd.domain.annotations;

import java.lang.annotation.*;

/**
 * Identifica um módulo DDD.
 *
 * @see <a href="https://domainlanguage.com/wp-content/uploads/2016/05/DDD_Reference_2015-03.pdf">Domain-Driven Design
 * Reference (Evans) - Bounded Contexts</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PACKAGE, ElementType.ANNOTATION_TYPE})
@Documented
public @interface DomainModule {

    /**
     * Um identificador estável para o módulo. Se não for definido, um identificador será derivado do elemento anotado,
     * geralmente um pacote. Isso permite que as ferramentas derivem o nome e a descrição, aplicando algum tipo de convenção ao
     * identificador.
     * <p>
     * Supondo um pacote {@code br.com.acme.app.module} anotado com {@code Module}, a ferramenta poderia usar um pacote de recursos
     * para pesquisar as chaves {@code br.com.acme.app.module._name} e {@code br.com.acme.app.module_description} para resolver
     * nome e descrição respectivamente.
     *
     * @return
     */
    String id() default "";

    /**
     * Um nome legível para o módulo. Pode ser substituído por um mecanismo de resolução externo via {@link #id()}.
     * O ferramental deve impedir que {@link #value()} e {@link #name()} sejam configurados ao mesmo tempo. Se em
     * dúvida, o valor definido em {@link #name()} será o preferido.
     *
     * @return
     * @see #id()
     */
    String name() default "";

    /**
     * Um alias para {@link #name()}. As ferramentas devem evitar que {@link #value()} e {@link #name()} sejam
     * configurados ao mesmo tempo. Em caso de dúvida, o valor definido em {@link #name()} será o preferido.
     *
     * @return
     * @see #name()
     */
    String value() default "";

    /**
     * Uma descrição legível por humanos para o módulo. Pode ser substituído por um mecanismo de resolução externo via
     * {@link #id()}.
     *
     * @return
     * @see #id()
     */
    String description() default "";
}
