package br.com.archbase.security.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Normalização da chave de contagem de tentativas.
 *
 * <p>A busca do usuário passa pelo banco, e em collation case-insensitive (padrão do MySQL)
 * {@code Vitima@x.com} e {@code vitima@x.com} autenticam contra a mesma linha. Sem normalizar,
 * cada variação de caixa ganharia um orçamento próprio de tentativas e o limitador nunca entraria.
 */
@DisplayName("Chave de rate limit")
class ArchbaseAuthRateLimiterKeyTest {

    private ArchbaseAuthRateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new ArchbaseAuthRateLimiter();
        ReflectionTestUtils.setField(limiter, "enabled", true);
        ReflectionTestUtils.setField(limiter, "maxAttempts", 3);
        ReflectionTestUtils.setField(limiter, "windowSeconds", 900L);
        ReflectionTestUtils.setField(limiter, "blockSeconds", 900L);
    }

    @Test
    @DisplayName("variações de caixa e espaço caem na mesma chave")
    void normalizaCaixaEEspaco() {
        String esperado = ArchbaseAuthRateLimiter.key("login", "vitima@x.com");

        assertThat(ArchbaseAuthRateLimiter.key("login", "Vitima@X.com")).isEqualTo(esperado);
        assertThat(ArchbaseAuthRateLimiter.key("login", "VITIMA@X.COM")).isEqualTo(esperado);
        assertThat(ArchbaseAuthRateLimiter.key("login", "  vitima@x.com  ")).isEqualTo(esperado);
    }

    @Test
    @DisplayName("escopos diferentes não se misturam")
    void escoposSeparados() {
        assertThat(ArchbaseAuthRateLimiter.key("login", "a@b.c"))
                .isNotEqualTo(ArchbaseAuthRateLimiter.key("reset", "a@b.c"));
    }

    @Test
    @DisplayName("identificador nulo não estoura")
    void identificadorNulo() {
        assertThat(ArchbaseAuthRateLimiter.key("login", null)).isEqualTo("login:");
    }

    @Test
    @DisplayName("o bloqueio alcança as variações de caixa — é o ponto da normalização")
    void bloqueioAlcancaVariacoes() {
        String chave = ArchbaseAuthRateLimiter.key("login", "Vitima@X.com");
        for (int i = 0; i < 3; i++) {
            limiter.recordFailure(chave);
        }

        assertThat(limiter.isBlocked(ArchbaseAuthRateLimiter.key("login", "vitima@x.com"))).isTrue();
    }
}
