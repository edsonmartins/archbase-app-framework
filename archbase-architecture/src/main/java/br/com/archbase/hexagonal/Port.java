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
 * Uma {@link Port} define um ponto de entrada na {@link Application} que pode acioná-la (consulte {@link PrimaryPort})
 * ou ser conduzido pelo aplicativo (consulte {@link SecondaryPort}). Eles são a interface com a qual o aplicativo
 * interage com o mundo exterior. {@link Port}s são implementados por {@link Adapter} usando integração específica
 * da tecnologia.
 *
 * @see <a href="https://alistair.cockburn.us/hexagonal-architecture/">Arquitetura Hexagonal</a>
 * @see Adapter
 * @see Application
 * @see PrimaryPort
 * @see SecondaryPort
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.PACKAGE, ElementType.TYPE })
@Documented
public @interface Port {}
