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
 * {@link Adapter}s contêm implementações específicas de tecnologia para conduzir (consulte {@link PrimaryPort}) ou implementar
 * {@link Port}s (consulte {@link SecondaryPort}). Os adaptadores não devem depender do código {@link Application} além de portas.
 *
 * @see <a href="https://alistair.cockburn.us/hexagonal-architecture/">Arquitetura Hexagonal</a>
 * @see Application
 * @see Port
 * @see PrimaryPort
 * @see SecondaryPort
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.PACKAGE, ElementType.TYPE })
@Documented
public @interface Adapter {}
