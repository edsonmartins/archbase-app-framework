package br.com.archbase.security;

import br.com.archbase.security.config.ArchbaseSecurityHardeningValidator;
import br.com.archbase.security.password.ArchbasePasswordStrengthPolicy;
import br.com.archbase.security.ratelimit.ArchbaseAuthRateLimiter;
import br.com.archbase.security.service.ArchbaseJwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static br.com.archbase.security.HardeningTestFixtures.validadorPadrao;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Garante que, com a configuração PADRÃO, nada do endurecimento de segurança altera o
 * comportamento anterior à auditoria.
 *
 * <p>Esta é a promessa central de toda a revisão: atualizar a dependência não pode quebrar
 * aplicação em produção. Cada proteção que muda o resultado de uma requisição fica atrás de uma
 * chave, e o padrão de cada chave é o comportamento antigo. Os testes abaixo travam esses padrões
 * — se alguém inverter um deles sem querer, quebra aqui e não no cliente.
 */
@DisplayName("Configuração padrão não altera comportamento anterior")
class DefaultsNaoQuebramTest {

    private static final String BASE64_KEY = Base64.getEncoder()
            .encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

    /** Reproduz os defaults declarados nos {@code @Value} das classes de produção. */
    private static <T> T comDefaults(T alvo, Map<String, Object> defaults) {
        defaults.forEach((campo, valor) -> ReflectionTestUtils.setField(alvo, campo, valor));
        return alvo;
    }

    @Nested
    @DisplayName("Política de força de senha")
    class PoliticaDeSenha {

        private ArchbasePasswordStrengthPolicy padrao() {
            return comDefaults(new ArchbasePasswordStrengthPolicy(), Map.of(
                    "minLength", 0,
                    "requireDigit", false,
                    "requireUppercase", false,
                    "requireLowercase", false,
                    "requireSpecial", false,
                    "blockCommon", false));
        }

        @Test
        @DisplayName("está inteiramente desligada")
        void desligada() {
            assertThat(padrao().isEnabled()).isFalse();
        }

        @Test
        @DisplayName("aceita senha fraca, comum, vazia e nula — como antes da auditoria")
        void aceitaOQueAceitavaAntes() {
            ArchbasePasswordStrengthPolicy policy = padrao();

            assertThatCode(() -> policy.validate("1")).doesNotThrowAnyException();
            assertThatCode(() -> policy.validate("senha")).doesNotThrowAnyException();
            assertThatCode(() -> policy.validate("123456")).doesNotThrowAnyException();
            assertThatCode(() -> policy.validate("")).doesNotThrowAnyException();
            assertThatCode(() -> policy.validate(null)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Separação de uso dos tokens JWT")
    class UsoDeToken {

        private ArchbaseJwtService padrao() {
            ArchbaseJwtService jwtService = new ArchbaseJwtService();
            ReflectionTestUtils.setField(jwtService, "secretKey", BASE64_KEY);
            ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3_600_000L);
            ReflectionTestUtils.setField(jwtService, "refreshExpiration", 86_400_000L);
            ReflectionTestUtils.setField(jwtService, "strictTokenUse", false);
            return jwtService;
        }

        /** Token no formato anterior à auditoria: assinado, válido, sem o claim token_use. */
        private String tokenLegado() {
            return Jwts.builder()
                    .subject("user@vendax.com.br")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                    .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(BASE64_KEY)), Jwts.SIG.HS256)
                    .compact();
        }

        @Test
        @DisplayName("sessão emitida antes da atualização continua valendo")
        void tokenLegadoContinuaValendo() {
            ArchbaseJwtService jwtService = padrao();
            String legado = tokenLegado();

            assertThat(jwtService.isAccessToken(legado)).isTrue();
            assertThat(jwtService.isRefreshToken(legado)).isTrue();
        }

        @Test
        @DisplayName("mas o desafio de MFA nunca passa como refresh, nem no modo compatível")
        void desafioMfaNuncaPassa() {
            ArchbaseJwtService jwtService = padrao();
            UserDetails user = User.withUsername("user@vendax.com.br").password("x").authorities("U").build();

            assertThat(jwtService.isRefreshToken(jwtService.generateMfaChallengeToken(user).token())).isFalse();
        }
    }

    @Nested
    @DisplayName("Rate limiting")
    class RateLimiting {

        @Test
        @DisplayName("vem LIGADO — é a única proteção ativa por padrão, com limite folgado")
        void ligadoPorPadrao() {
            ArchbaseAuthRateLimiter limiter = comDefaults(new ArchbaseAuthRateLimiter(), Map.of(
                    "enabled", true,
                    "maxAttempts", 10,
                    "windowSeconds", 900L,
                    "blockSeconds", 900L));

            String chave = ArchbaseAuthRateLimiter.key("login", "user@x.com");
            // Nove erros seguidos não trancam: usuário legítimo não chega perto disso.
            for (int i = 0; i < 9; i++) {
                limiter.recordFailure(chave);
            }
            assertThat(limiter.isBlocked(chave)).isFalse();

            limiter.recordFailure(chave);
            assertThat(limiter.isBlocked(chave)).isTrue();
        }
    }

    @Nested
    @DisplayName("Validador de pré-requisitos")
    class Validador {

        @Test
        @DisplayName("com todos os defaults, não impede a subida")
        void defaultsNaoImpedemSubida() {
            ArchbaseSecurityHardeningValidator validator = validadorPadrao(0L);

            assertThatCode(validator::validate).doesNotThrowAnyException();
        }
    }

}
