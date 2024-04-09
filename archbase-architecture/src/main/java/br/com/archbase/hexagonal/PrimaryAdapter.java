/*
 * Copyright 2022 the original author or authors.
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
package br.com.archbase.hexagonal;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Um {@link PrimaryAdapter} conecta a parte externa de um aplicativo a um {@link PrimaryPort} exposto pelo
 * núcleo do aplicativo. Por exemplo, pode ser um componente aceitando solicitações HTTP ou um ouvinte para um agente de mensagens.
 *
 * @see <a href="https://alistair.cockburn.us/hexagonal-architecture/">Arquitetura Hexagonal</a>
 * @see PrimaryPort
 */
@Adapter
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.PACKAGE, ElementType.TYPE })
@Documented
public @interface PrimaryAdapter {}
