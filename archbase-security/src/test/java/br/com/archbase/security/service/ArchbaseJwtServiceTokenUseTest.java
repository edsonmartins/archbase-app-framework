package br.com.archbase.security.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Separação de uso dos tokens ({@code token_use}).
 *
 * <p>O caso que originou estes testes: o desafio de MFA é um JWT assinado com o subject do usuário,
 * emitido depois de a senha conferir e antes do segundo fator. Enquanto o
 * {@code /auth/refresh-token} olhava só assinatura + subject, trocá-lo lá devolvia os tokens reais
 * — o segundo fator deixava de existir.
 */
@DisplayName("ArchbaseJwtService — separação de uso dos tokens")
class ArchbaseJwtServiceTokenUseTest {

    private static final String BASE64_KEY = Base64.getEncoder()
            .encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

    private final ArchbaseJwtService jwtService = new ArchbaseJwtService();
    private final UserDetails user = User.withUsername("user@vendax.com.br")
            .password("x").authorities("ROLE_USER").build();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secretKey", BASE64_KEY);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3_600_000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 86_400_000L);
        ReflectionTestUtils.setField(jwtService, "strictTokenUse", false);
    }

    @Test
    @DisplayName("desafio de MFA não é aceito como refresh token")
    void desafioMfaNaoServeComoRefresh() {
        String challenge = jwtService.generateMfaChallengeToken(user).token();

        assertThat(jwtService.isRefreshToken(challenge)).isFalse();
        assertThat(jwtService.isAccessToken(challenge)).isFalse();
    }

    @Test
    @DisplayName("access token não é aceito como refresh, e refresh não é aceito como access")
    void accessERefreshNaoSeConfundem() {
        String access = jwtService.generateToken(user).token();
        String refresh = jwtService.generateRefreshToken(user).token();

        assertThat(jwtService.isAccessToken(access)).isTrue();
        assertThat(jwtService.isRefreshToken(access)).isFalse();

        assertThat(jwtService.isRefreshToken(refresh)).isTrue();
        assertThat(jwtService.isAccessToken(refresh)).isFalse();
    }

    @Test
    @DisplayName("token emitido por esta versão sempre declara o uso")
    void tokensNovosDeclaramUso() {
        assertThat(jwtService.extractTokenUse(jwtService.generateToken(user).token()))
                .isEqualTo(ArchbaseJwtService.TOKEN_USE_ACCESS);
        assertThat(jwtService.extractTokenUse(jwtService.generateRefreshToken(user).token()))
                .isEqualTo(ArchbaseJwtService.TOKEN_USE_REFRESH);
        assertThat(jwtService.extractTokenUse(jwtService.generateMfaChallengeToken(user).token()))
                .isEqualTo(ArchbaseJwtService.TOKEN_USE_MFA_CHALLENGE);
    }

    @Nested
    @DisplayName("token legado (emitido antes do claim existir)")
    class TokenLegado {

        /** Reproduz o formato anterior: assinado e válido, porém sem {@code token_use}. */
        private String tokenLegado() {
            return Jwts.builder()
                    .subject("user@vendax.com.br")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                    .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(BASE64_KEY)), Jwts.SIG.HS256)
                    .compact();
        }

        @Test
        @DisplayName("é aceito enquanto strict-token-use está desligado — a atualização não derruba sessão em curso")
        void aceitoPorPadrao() {
            String legado = tokenLegado();

            assertThat(jwtService.extractTokenUse(legado)).isNull();
            assertThat(jwtService.isAccessToken(legado)).isTrue();
            assertThat(jwtService.isRefreshToken(legado)).isTrue();
        }

        @Test
        @DisplayName("é recusado com strict-token-use ligado")
        void recusadoNoModoEstrito() {
            ReflectionTestUtils.setField(jwtService, "strictTokenUse", true);
            String legado = tokenLegado();

            assertThat(jwtService.isAccessToken(legado)).isFalse();
            assertThat(jwtService.isRefreshToken(legado)).isFalse();
        }

        @Test
        @DisplayName("desafio de MFA legado continua recusado como refresh — a tolerância não o alcança")
        void desafioLegadoNaoEscapa() {
            // Desafio no formato antigo: tem mfa=challenge, mas nenhum token_use.
            String desafioLegado = Jwts.builder()
                    .claims(Map.of(ArchbaseJwtService.MFA_PURPOSE_CLAIM, ArchbaseJwtService.MFA_CHALLENGE_VALUE))
                    .subject("user@vendax.com.br")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 300_000L))
                    .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(BASE64_KEY)), Jwts.SIG.HS256)
                    .compact();

            assertThat(jwtService.extractTokenUse(desafioLegado)).isNull();
            assertThat(jwtService.isRefreshToken(desafioLegado)).isFalse();
        }
    }

    @Test
    @DisplayName("entrada que não é JWT não passa por nenhum uso")
    void lixoNaoPassa() {
        assertThat(jwtService.isAccessToken("nao-e-um-jwt")).isFalse();
        assertThat(jwtService.isRefreshToken("nao-e-um-jwt")).isFalse();
    }
}
