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
package br.com.archbase.layered;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifica o {@link InfrastructureLayer} em uma arquitetura em camadas. A camada de infraestrutura suporta a outras
 * camadas, fornecendo recursos técnicos, como persistência ou envio de mensagens. Além disso, também pode apoiar
 * as outras camadas em suas interações, fornecendo funcionalidades de estrutura.
 * <p>
 * A funcionalidade de infraestrutura fornecida é específica da camada, ou seja, a funcionalidade fornecida para {@link InterfaceLayer}
 * (por exemplo REST) não deve ser acessado pelas camadas abaixo.
 *
 * @see <a href="https://domainlanguage.com/wp-content/uploads/2016/05/DDD_Reference_2015-03.pdf">Domain-Driven Design
 *      Reference (Evans) - Layered Architecture</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.PACKAGE, ElementType.TYPE })
@Documented
public @interface InfrastructureLayer {
}
