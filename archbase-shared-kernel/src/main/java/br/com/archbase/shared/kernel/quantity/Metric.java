/*
 * Copyright 2017-2019 the original author or authors.
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
package br.com.archbase.shared.kernel.quantity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;

/**
 * Todas as métricas disponíveis.
 */
@Getter
public enum Metric {

    SQUARE_METER("m²", "m2"), METER("m"), KILOGRAM("kg"), LITER("l"), UNIT("");

    private final String abbreviation;
    private final List<String> abbreviations;

    /**
     * Cria uma nova {@link Metric} com a abreviatura principal fornecida e possivelmente outras adicionais que são usadas para
     * analisar instâncias {@link Metric} de fontes {@link String}.
     *
     * @param primaryAbbreviation     não deve ser {@literal null}.
     * @param additionalAbbreviations não deve ser {@literal null}, consulte {@link #from (String)}.
     */
    private Metric(String primaryAbbreviation, String... additionalAbbreviations) {

        Assert.notNull(primaryAbbreviation, "Abreviação primária não deve ser nula!");
        Assert.notNull(additionalAbbreviations, "Abreviações adicionais não devem ser nulas!");

        this.abbreviation = primaryAbbreviation;
        this.abbreviations = Arrays.asList(additionalAbbreviations);
    }

    /**
     * Retorna a {@link Metric} para a abreviatura fornecida.
     *
     * @param abbreviation A abreviatura  não deve ser {@literal null}.
     * @return nunca será {@literal null}.
     * @throws IllegalArgumentException se nenhuma {@link Metric} puder ser encontrada para a abreviação fornecida.
     */
    public static Metric from(String abbreviation) {

        Assert.notNull(abbreviation, "A fonte da abreviatura não deve ser nula!");

        String source = abbreviation.trim();

        return Arrays.stream(Metric.values()) //
                .filter(it -> it.abbreviation.equals(source) || it.abbreviations.contains(source)) //
                .findFirst() //
                .orElseThrow(() -> new IllegalArgumentException(String.format("Abreviatura não suportada %s!", abbreviation)));
    }

    /**
     * Retorna se a {@link Metric} fornecida é
     *
     * @param metric não deve ser {@literal null}.
     * @return
     */
    public boolean isCompatibleWith(Metric metric) {

        Assert.notNull(metric, "A métrica não deve ser nula!");
        return this.equals(metric);
    }
}
