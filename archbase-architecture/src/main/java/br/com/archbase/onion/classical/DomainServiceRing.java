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
package br.com.archbase.onion.classical;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifica o {@link DomainServiceRing} em uma arquitetura de cebola. O anel de serviço de domínio define as interfaces
 * necessárias para, por ex. armazenar e recuperar dados, ou seja, interfaces de repositório. No entanto, não fornece as informações técnicas
 * e implementações. Eles serão fornecidos pelo {@link InfrastructureRing}. Assim, o anel de serviço de domínio suporta a
 * execução de casos de uso de negócios.
 *
 * @see <a href="https://jeffreypalermo.com/2008/07/the-onion-architecture-part-1/">A arquitetura Onion: parte 1 (Palermo)</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.PACKAGE, ElementType.TYPE })
@Documented
public @interface DomainServiceRing {
}
