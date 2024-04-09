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
 * Identifica um {@link DomainRepository}. Repositórios simulam uma coleção de agregados para os quais as instâncias agregadas podem ser
 * adicionados, atualizadas e removidos. Eles geralmente também expõem a API para selecionar um subconjunto de agregados
 * que correspondem a certos critérios. Acesso a projeções de um agregado também podem ser fornecidas,
 * mas também por meio de uma abstração separada dedicada.
 * <p>
 * As implementações usam um mecanismo de persistência dedicado apropriado para a estrutura de dados e requisitos de consulta em
 * mão. No entanto, eles devem se certificar de que nenhuma API específica do mecanismo de persistência vaze para o código do cliente.
 *
 * @see DomainAggregateRoot
 * @see <a href="https://domainlanguage.com/wp-content/uploads/2016/05/DDD_Reference_2015-03.pdf">Domain-Driven Design
 * Reference (Evans) - Repositories</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface DomainRepository {

}
