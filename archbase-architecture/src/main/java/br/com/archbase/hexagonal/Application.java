/*
 * Copyright 2022 the original author or authors.
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
package br.com.archbase.hexagonal;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Uma anotação para atribuir a pacotes e tipos a função do código do aplicativo principal. Esse código não deve referir-se a nenhum
 * código {@link Adapter}, mas apenas expõe ou depende da funcionalidade por meio de {@link Port} conectando a
 * aplicação ao mundo exterior.
 *
 * @see <a href="https://alistair.cockburn.us/hexagonal-architecture/">Arquitetura Hexagonal</a>
 * @see Port
 * @see Adapter
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.PACKAGE, ElementType.TYPE })
@Documented
public @interface Application {}
