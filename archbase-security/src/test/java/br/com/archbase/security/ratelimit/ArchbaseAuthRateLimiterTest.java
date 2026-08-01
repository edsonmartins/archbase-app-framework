package br.com.archbase.security.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ArchbaseAuthRateLimiter")
class ArchbaseAuthRateLimiterTest {

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
    @DisplayName("bloqueia ao atingir o limite de tentativas")
    void bloqueiaNoLimite() {
        assertThat(limiter.isBlocked("login:a@b.c")).isFalse();

        limiter.recordFailure("login:a@b.c");
        limiter.recordFailure("login:a@b.c");
        assertThat(limiter.isBlocked("login:a@b.c")).isFalse();

        limiter.recordFailure("login:a@b.c");
        assertThat(limiter.isBlocked("login:a@b.c")).isTrue();
        assertThat(limiter.secondsUntilUnblock("login:a@b.c")).isPositive();
    }

    @Test
    @DisplayName("sucesso zera a contagem — usuário legítimo não herda tentativas de um atacante")
    void sucessoZeraContagem() {
        limiter.recordFailure("login:a@b.c");
        limiter.recordFailure("login:a@b.c");
        limiter.recordSuccess("login:a@b.c");

        limiter.recordFailure("login:a@b.c");
        limiter.recordFailure("login:a@b.c");
        assertThat(limiter.isBlocked("login:a@b.c")).isFalse();
    }

    @Test
    @DisplayName("o bloqueio é por chave — travar um e-mail não trava os outros")
    void bloqueioIsoladoPorChave() {
        for (int i = 0; i < 3; i++) {
            limiter.recordFailure("login:vitima@b.c");
        }

        assertThat(limiter.isBlocked("login:vitima@b.c")).isTrue();
        assertThat(limiter.isBlocked("login:outro@b.c")).isFalse();
    }

    @Test
    @DisplayName("bloqueio vence e libera de novo")
    void bloqueioExpira() {
        ReflectionTestUtils.setField(limiter, "blockSeconds", 0L);
        for (int i = 0; i < 3; i++) {
            limiter.recordFailure("login:a@b.c");
        }

        assertThat(limiter.isBlocked("login:a@b.c")).isFalse();
    }

    @Test
    @DisplayName("desligado, não bloqueia nunca")
    void desligadoNaoBloqueia() {
        ReflectionTestUtils.setField(limiter, "enabled", false);
        for (int i = 0; i < 50; i++) {
            limiter.recordFailure("login:a@b.c");
        }

        assertThat(limiter.isBlocked("login:a@b.c")).isFalse();
    }
}
