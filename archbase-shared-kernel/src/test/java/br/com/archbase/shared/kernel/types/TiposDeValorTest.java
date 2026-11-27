package br.com.archbase.shared.kernel.types;

import br.com.archbase.shared.kernel.converters.LocalTimeStringConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("shared-kernel: Email, CreditCard, Quantity e LocalTimeStringConverter")
class TiposDeValorTest {

    @Test
    @DisplayName("Email.of confere o formato; instance continua aceitando")
    void email() {
        assertThat(Email.of("ana@empresa.com.br").isValido()).isTrue();
        assertThatThrownBy(() -> Email.of("ana@")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Email.of("sem-arroba.com")).isInstanceOf(IllegalArgumentException.class);
        assertThat(Email.instance("ana@").isValido()).as("instance não recusa, mas isValido diz a verdade").isFalse();
    }

    @Test
    @DisplayName("CreditCard.of confere Luhn, mês, ano e código; instance continua aceitando")
    void cartao() {
        assertThat(CreditCard.of("4111 1111 1111 1111", "123", "12", "29").isValido()).isTrue();
        assertThatThrownBy(() -> CreditCard.of("4111111111111112", "123", "12", "29")).as("Luhn")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CreditCard.of("4111111111111111", "123", "13", "29")).as("mês")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CreditCard.of("4111111111111111", "12", "12", "29")).as("código")
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(CreditCard.instance("123", "1", "99", "x").isValido()).isFalse();
    }

    @Test
    @DisplayName("Quantity soma sem passar por double e compara por valor")
    void quantidade() {
        Quantity soma = new Quantity("0.1").add(new Quantity("0.2"));
        assertThat(soma.getValue()).isEqualByComparingTo(new BigDecimal("0.3"));
        assertThat(new Quantity("1.0")).isEqualTo(new Quantity("1.00")).hasSameHashCodeAs(new Quantity("1.00"));
        assertThat(new Quantity("5").subtract(new Quantity("0.7"))).isEqualTo(new Quantity("4.3"));
    }

    @Test
    @DisplayName("LocalTimeStringConverter grava null como null (antes, NullPointerException)")
    void horaNula() {
        var conversor = new LocalTimeStringConverter();
        assertThat(conversor.convertToDatabaseColumn(null)).isNull();
        assertThat(conversor.convertToDatabaseColumn(LocalTime.of(9, 5))).isEqualTo("09:05");
    }
}
