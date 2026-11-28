package br.com.archbase.ddd.domain.specification.basic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code isSatisfiedBy} de Like/NotLike segue o {@code like} do banco. Até a 3.5.0 testava se o padrão continha o
 * valor, e as duas specifications davam o mesmo resultado.
 */
@DisplayName("Like e NotLike em memória")
class LikeEmMemoriaTest {

    static class Cliente {
        final String nome;

        Cliente(String nome) {
            this.nome = nome;
        }
    }

    @ParameterizedTest(name = "\"{0}\" like \"{1}\" = {2}")
    @CsvSource({
            "Anabela, Ana%,  true",
            "Ana,     Ana%,  true",
            "Bruno,   Ana%,  false",
            "Mariana, %ana,  true",
            "Ana,     A_a,   true",
            "Anna,    A_a,   false",
            "Ana,     Ana,   true",
            "An,      Ana,   false",
            "a.b,     a.b,   true",
            "axb,     a.b,   false",
            "50%,     50%,   true"
    })
    void like(String valor, String padrao, boolean esperado) {
        Cliente c = new Cliente(valor);
        assertThat(new LikeArchbaseSpecification<Cliente>("nome", padrao).isSatisfiedBy(c)).isEqualTo(esperado);
        assertThat(new NotLikeArchbaseSpecification<Cliente>("nome", padrao).isSatisfiedBy(c)).isEqualTo(!esperado);
    }
}
