package br.com.archbase.valueobject.document;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Dígitos verificadores de CPF e CNPJ. Até a 3.5.0 o primeiro dígito era calculado com os pesos do segundo, e
 * {@code CPF.of}/{@code CNPJ.of} recusavam todo documento válido.
 */
@DisplayName("CPF e CNPJ: dígitos verificadores")
class DigitosVerificadoresTest {

    private static final String ALFANUMERICO = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    @ParameterizedTest
    @ValueSource(strings = {"529.982.247-25", "52998224725", "123.456.789-09", "111.444.777-35", "000.000.001-91"})
    @DisplayName("CPF válido é aceito, com ou sem máscara")
    void cpfValido(String numero) {
        assertThat(CPF.of(numero).valido()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"529.982.247-26", "529.982.247-15", "111.111.111-11", "000.000.000-00"})
    @DisplayName("CPF com dígito errado ou repetido é recusado")
    void cpfInvalido(String numero) {
        assertThat(CPF.ofSemValidacao(numero).valido()).isFalse();
        assertThatThrownBy(() -> CPF.of(numero)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"11.222.333/0001-81", "11222333000181", "45.997.418/0001-53", "00.000.000/0001-91"})
    @DisplayName("CNPJ numérico válido é aceito, com ou sem máscara")
    void cnpjNumericoValido(String numero) {
        assertThat(CNPJ.of(numero).valido()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12.ABC.345/01DE-35", "12ABC34501DE35", "12.abc.345/01de-35"})
    @DisplayName("CNPJ alfanumérico válido é aceito; letras minúsculas são convertidas")
    void cnpjAlfanumericoValido(String numero) {
        CNPJ cnpj = CNPJ.of(numero);
        assertThat(cnpj.numeroLimpo()).isEqualTo("12ABC34501DE35");
        assertThat(cnpj.numero()).isEqualTo("12.ABC.345/01DE-35");
    }

    @ParameterizedTest
    @ValueSource(strings = {"11.222.333/0001-82", "11.222.333/0001-91", "12.ABC.345/01DE-36", "11.111.111/1111-11",
            "AA.AAA.AAA/AAAA-00"})
    @DisplayName("CNPJ com dígito errado ou repetido é recusado")
    void cnpjInvalido(String numero) {
        assertThat(CNPJ.ofSemValidacao(numero).valido()).isFalse();
        assertThatThrownBy(() -> CNPJ.of(numero)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"12.ABC.345/01DE-3A", "1234567890123", "123456789012345"})
    @DisplayName("CNPJ fora do formato não é construído: dígito verificador com letra, tamanho errado")
    void cnpjForaDoFormato(String numero) {
        assertThatThrownBy(() -> CNPJ.ofSemValidacao(numero)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("CPF: para 2 mil bases aleatórias, exatamente o mesmo par de dígitos que o Stella aceita")
    void cpfConfereComStella() {
        var stella = new br.com.caelum.stella.validation.CPFValidator(false);
        Random random = new Random(20261124);
        for (int n = 0; n < 2000; n++) {
            String base = aleatorio(random, 9, "0123456789");
            for (int dv = 0; dv < 100; dv++) {
                String numero = base + String.format("%02d", dv);
                boolean esperado = stella.invalidMessagesFor(numero).isEmpty();
                assertThat(CPF.ofSemValidacao(numero).valido()).as(numero).isEqualTo(esperado);
            }
        }
    }

    @Test
    @DisplayName("CNPJ numérico e alfanumérico: para 2 mil bases aleatórias, o mesmo resultado que o Stella")
    void cnpjConfereComStella() {
        var stella = new br.com.caelum.stella.validation.CNPJValidator(false);
        Random random = new Random(20261124);
        for (int n = 0; n < 2000; n++) {
            String base = aleatorio(random, 12, n % 2 == 0 ? "0123456789" : ALFANUMERICO);
            for (int dv = 0; dv < 100; dv++) {
                String numero = base + String.format("%02d", dv);
                boolean esperado = stella.invalidMessagesFor(numero).isEmpty();
                assertThat(CNPJ.ofSemValidacao(numero).valido()).as(numero).isEqualTo(esperado);
            }
        }
    }

    private static String aleatorio(Random random, int tamanho, String alfabeto) {
        StringBuilder sb = new StringBuilder(tamanho);
        for (int i = 0; i < tamanho; i++) {
            sb.append(alfabeto.charAt(random.nextInt(alfabeto.length())));
        }
        return sb.toString();
    }
}
