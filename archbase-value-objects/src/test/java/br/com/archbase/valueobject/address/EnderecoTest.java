package br.com.archbase.valueobject.address;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Endereco.cidadeUf")
class EnderecoTest {

    @Test
    @DisplayName("só cidade ou só UF não vira \"X-null\"")
    void parcial() {
        assertThat(Endereco.builder().cidade("Campinas").build().cidadeUf()).isEqualTo("Campinas");
        assertThat(Endereco.builder().uf("SP").build().cidadeUf()).isEqualTo("SP");
        assertThat(Endereco.builder().cidade("Campinas").uf("SP").build().cidadeUf()).isEqualTo("Campinas-SP");
        assertThat(Endereco.builder().build().cidadeUf()).isNull();
    }
}
