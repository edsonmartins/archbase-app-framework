package br.com.archbase.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Token de desafio de MFA no {@link ArchbaseJwtService}: carrega o claim {@code mfa=challenge}
 * (distinguível de um access token) e valida corretamente.
 */
@DisplayName("ArchbaseJwtService — token de desafio de MFA")
class ArchbaseJwtServiceMfaTest {

    private final ArchbaseJwtService jwtService = new ArchbaseJwtService();
    private final UserDetails user = User.withUsername("user@vendax.com.br")
            .password("x").authorities("ROLE_USER").build();

    @BeforeEach
    void setUp() {
        String base64Key = Base64.getEncoder()
                .encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));
        ReflectionTestUtils.setField(jwtService, "secretKey", base64Key);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3_600_000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 86_400_000L);
    }

    @Test
    @DisplayName("desafio é reconhecido; access token comum não é desafio")
    void challengeVsAccess() {
        String challenge = jwtService.generateMfaChallengeToken(user).token();
        String access = jwtService.generateToken(user).token();

        assertThat(jwtService.isMfaChallengeToken(challenge)).isTrue();
        assertThat(jwtService.isMfaChallengeToken(access)).isFalse();
        assertThat(jwtService.extractUsername(challenge)).isEqualTo("user@vendax.com.br");
    }

    @Test
    @DisplayName("token inválido/aleatório não passa como desafio")
    void lixoNaoEhDesafio() {
        assertThat(jwtService.isMfaChallengeToken("nao-e-um-jwt")).isFalse();
    }
}
