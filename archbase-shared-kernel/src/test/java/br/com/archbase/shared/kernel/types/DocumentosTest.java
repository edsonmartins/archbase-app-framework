package br.com.archbase.shared.kernel.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("shared-kernel: CPF e CNPJ")
class DocumentosTest {

    @Test
    @DisplayName("CPF: of confere os dígitos; instance continua aceitando sem conferir")
    void cpf() {
        assertThat(CPF.of("529.982.247-25").isValido()).isTrue();
        assertThatThrownBy(() -> CPF.of("529.982.247-26")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CPF.of("111.111.111-11")).isInstanceOf(IllegalArgumentException.class);

        CPF semConferir = CPF.instance("529.982.247-26");
        assertThat(semConferir.isValido()).isFalse();
    }

    @Test
    @DisplayName("CPF: instance guarda sem máscara, como o setter, e formata")
    void cpfSemMascara() {
        CPF cpf = CPF.instance("529.982.247-25");
        assertThat(cpf.getInternalCpf()).isEqualTo("52998224725");
        assertThat(cpf.getCpfFormadado()).isEqualTo("529.982.247-25");
        assertThat(cpf).isEqualTo(CPF.instance("52998224725"));
    }

    @Test
    @DisplayName("CNPJ formatado deixa de ser recusado: a máscara sai antes de conferir o tamanho")
    void cnpjFormatado() {
        CNPJ cnpj = CNPJ.instance("11.222.333/0001-81");
        assertThat(cnpj.getInternalCnpj()).isEqualTo("11222333000181");
        assertThat(cnpj.getCnpjFormatado()).isEqualTo("11.222.333/0001-81");
    }

    @Test
    @DisplayName("CNPJ: of confere os dígitos, numérico e alfanumérico")
    void cnpj() {
        assertThat(CNPJ.of("11.222.333/0001-81").isValido()).isTrue();
        assertThat(CNPJ.of("45.997.418/0001-53").isValido()).isTrue();
        assertThat(CNPJ.of("12.abc.345/01de-35").getCnpjFormatado()).isEqualTo("12.ABC.345/01DE-35");
        assertThatThrownBy(() -> CNPJ.of("11.222.333/0001-82")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CNPJ.of("12.ABC.345/01DE-36")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CNPJ.of("00.000.000/0000-00")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("CNPJ: instance e setter mantêm a regra antiga (só o tamanho), sem passar a recusar dígito errado")
    void cnpjCompatibilidade() {
        CNPJ cnpj = CNPJ.instance("11222333000182");
        assertThat(cnpj.isValido()).isFalse();

        CNPJ vazio = CNPJ.instance();
        vazio.setInternalCnpj("11.222.333/0001-82");
        assertThat(vazio.getInternalCnpj()).isEqualTo("11222333000182");

        assertThatThrownBy(() -> CNPJ.instance("123")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> vazio.setInternalCnpj(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
