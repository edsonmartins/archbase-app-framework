package br.com.archbase.security.service;

import br.com.archbase.security.service.ArchbaseCapabilityDependencyService.Qualificadas;
import br.com.archbase.security.util.CapabilityRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A leitura de {@code requires} — <b>uma só</b>, compartilhada pelos dois coletores.
 *
 * <p>A varredura de {@code @HasPermission} e o registro de tela declaram dependências no mesmo
 * formato. Duas implementações da mesma interpretação já produziram, neste módulo, o defeito de
 * catalogar com um nome e consultar com outro — daí a função ser única e estes testes cobrirem-na
 * diretamente, em vez de cobrirem cada coletor por fora.
 */
@DisplayName("Qualificação de dependência")
class QualificacaoDeDependenciaTest {

    @Nested
    @DisplayName("formas aceitas")
    class FormasAceitas {

        @Test
        @DisplayName("a forma curta resolve contra o recurso de quem declara")
        void formaCurta() {
            Qualificadas r = ArchbaseCapabilityDependencyService.qualificar(
                    List.of("view"), "tms.ordemservico", "aprovar_custo");

            assertThat(r.validas()).containsExactly("tms.ordemservico:view");
            assertThat(r.invalidas()).isEmpty();
        }

        @Test
        @DisplayName("a forma qualificada aponta para outro recurso")
        void formaQualificada() {
            Qualificadas r = ArchbaseCapabilityDependencyService.qualificar(
                    List.of("tms.cliente:view"), "tms.ordemservico", "aprovar_custo");

            assertThat(r.validas()).containsExactly("tms.cliente:view");
        }

        @Test
        @DisplayName("espaço em volta não muda a referência")
        void espacoEmVolta() {
            Qualificadas r = ArchbaseCapabilityDependencyService.qualificar(
                    List.of("  tms.cliente : view  "), "tms.ordemservico", "aprovar_custo");

            assertThat(r.validas()).containsExactly("tms.cliente:view");
        }

        @Test
        @DisplayName("a mesma dependência declarada duas vezes vira uma aresta")
        void duplicataColapsa() {
            Qualificadas r = ArchbaseCapabilityDependencyService.qualificar(
                    List.of("view", "tms.ordemservico:view"), "tms.ordemservico", "aprovar_custo");

            assertThat(r.validas()).containsExactly("tms.ordemservico:view");
        }
    }

    @Nested
    @DisplayName("o que é descartado")
    class Descartado {

        @Test
        @DisplayName("dois separadores é ambiguidade — não há como saber onde termina o recurso")
        void doisSeparadores() {
            // Escolher uma das leituras catalogaria a aresta errada em silêncio.
            Qualificadas r = ArchbaseCapabilityDependencyService.qualificar(
                    List.of("a:b:c"), "tms.ordemservico", "aprovar_custo");

            assertThat(r.validas()).isEmpty();
            assertThat(r.invalidas()).containsExactly("a:b:c");
        }

        @Test
        @DisplayName("lado vazio do separador não é capacidade")
        void ladoVazio() {
            Qualificadas r = ArchbaseCapabilityDependencyService.qualificar(
                    List.of(":view", "tms.cliente:", "  "), "tms.ordemservico", "aprovar_custo");

            assertThat(r.validas()).isEmpty();
            assertThat(r.invalidas()).hasSize(3);
        }

        @Test
        @DisplayName("forma curta sem recurso de origem não resolve")
        void curtaSemRecurso() {
            Qualificadas r = ArchbaseCapabilityDependencyService.qualificar(
                    List.of("view"), null, "aprovar_custo");

            assertThat(r.validas()).isEmpty();
            assertThat(r.invalidas()).containsExactly("view");
        }

        @Test
        @DisplayName("dependência de si mesma é redundância, e sai separada do inválido")
        void autoReferencia() {
            // Não é erro: gravar a aresta faria a tela sugerir conceder o que já está sendo
            // concedido. Mas também não é entrada malformada, e quem avisa usa outro tom.
            Qualificadas r = ArchbaseCapabilityDependencyService.qualificar(
                    List.of("aprovar_custo", "tms.ordemservico:aprovar_custo", "view"),
                    "tms.ordemservico", "aprovar_custo");

            assertThat(r.validas()).containsExactly("tms.ordemservico:view");
            assertThat(r.autoReferencias()).containsExactly(
                    "tms.ordemservico:aprovar_custo", "tms.ordemservico:aprovar_custo");
            assertThat(r.invalidas()).isEmpty();
        }

        @Test
        @DisplayName("nulo não é declaração — devolve tudo vazio, sem estourar")
        void nuloNaoDeclara() {
            Qualificadas r = ArchbaseCapabilityDependencyService.qualificar(
                    null, "tms.ordemservico", "aprovar_custo");

            assertThat(r.validas()).isEmpty();
            assertThat(r.invalidas()).isEmpty();
        }
    }

    @Nested
    @DisplayName("CapabilityRef")
    class Referencia {

        @Test
        @DisplayName("compõe e decompõe a forma canônica")
        void idaEVolta() {
            String ref = CapabilityRef.de("tms.ordemservico", "aprovar_custo");

            assertThat(ref).isEqualTo("tms.ordemservico:aprovar_custo");
            assertThat(CapabilityRef.recursoDe(ref)).isEqualTo("tms.ordemservico");
            assertThat(CapabilityRef.acaoDe(ref)).isEqualTo("aprovar_custo");
        }
    }
}
