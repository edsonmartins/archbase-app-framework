package br.com.archbase.controlplane.token;

import br.com.archbase.controlplane.it.FakeControlPlane;
import br.com.archbase.controlplane.session.AdminRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ControlPlaneTokenValidatorTest {

    private FakeControlPlane cp;
    private JwksKeyStore store;

    @BeforeEach
    void setUp() {
        cp = new FakeControlPlane().addKey("k1", true);
        store = new JwksKeyStore(cp.jwksUrl(), null, Duration.ZERO, Duration.ofSeconds(2), Clock.systemUTC());
        store.initialize();
    }

    @AfterEach
    void tearDown() {
        cp.close();
    }

    private ControlPlaneTokenValidator validator(boolean seatRequired) {
        return new ControlPlaneTokenValidator(store, FakeControlPlane.ISSUER, FakeControlPlane.AUDIENCE,
                Duration.ofSeconds(30), seatRequired);
    }

    @Test
    void leTodasAsClaims() {
        String token = cp.session("k1", "u1", "u1@x.com", "t1", "tenant_admin").claim("seat", "s-9").compact();
        TokenValidationResult result = validator(false).validate(token);

        assertThat(result.isValid()).isTrue();
        ControlPlaneClaims claims = result.claims();
        assertThat(claims.use()).isEqualTo(ControlPlaneClaims.TokenUse.SESSION);
        assertThat(claims.subject()).isEqualTo("u1");
        assertThat(claims.email()).isEqualTo("u1@x.com");
        assertThat(claims.tenantId()).isEqualTo("t1");
        assertThat(claims.adminRole()).isEqualTo(AdminRole.TENANT_ADMIN);
        assertThat(claims.seatId()).isEqualTo("s-9");
        assertThat(claims.keyId()).isEqualTo("k1");
        assertThat(claims.tokenId()).isNotBlank();
    }

    @Test
    void produtoComAssentoObrigatorioRecusaTokenSemSeat() {
        String semSeat = cp.session("k1", "u1", "u1@x.com", "t1", "member").compact();
        assertThat(validator(true).validate(semSeat).code()).isEqualTo(TokenValidationResult.NO_SEAT);
        assertThat(validator(true).validate(semSeat).status()).isEqualTo(403);
        assertThat(validator(false).validate(semSeat).isValid()).isTrue();

        String comSeat = cp.session("k1", "u1", "u1@x.com", "t1", "member").claim("seat", "s1").compact();
        assertThat(validator(true).validate(comSeat).isValid()).isTrue();
    }

    @Test
    void admAusenteViraMemberEDesconhecidoERecusado() {
        String semAdm = cp.session("k1", "u1", "u1@x.com", "t1", null).compact();
        assertThat(validator(false).validate(semAdm).claims().adminRole()).isEqualTo(AdminRole.MEMBER);

        String admEstranho = cp.session("k1", "u1", "u1@x.com", "t1", "root").compact();
        assertThat(validator(false).validate(admEstranho).code()).isEqualTo(TokenValidationResult.INVALID_TOKEN);
    }

    @Test
    void tokenUseDesconhecidoERecusado() {
        String token = cp.session("k1", "u1", "u1@x.com", "t1", "member").claim("token_use", "refresh").compact();
        assertThat(validator(false).validate(token).code()).isEqualTo(TokenValidationResult.INVALID_TOKEN);
    }

    @Test
    void tokenSemExpERecusado() {
        String token = cp.session("k1", "u1", "u1@x.com", "t1", "member").expiresAt(null).compact();
        assertThat(validator(false).validate(token).code()).isEqualTo(TokenValidationResult.INVALID_TOKEN);
    }

    @Test
    void reconheceSoJwsEdDsa() {
        ControlPlaneTokenValidator v = validator(false);
        assertThat(v.looksLikeControlPlaneToken(cp.session("k1", "u1", "e", "t1", "member").compact())).isTrue();
        // cabeçalho de um JWT HS256 do Archbase: {"alg":"HS256"}
        assertThat(v.looksLikeControlPlaneToken("eyJhbGciOiJIUzI1NiJ9.e30.x")).isFalse();
        assertThat(v.looksLikeControlPlaneToken("nao-e-jwt")).isFalse();
        assertThat(v.looksLikeControlPlaneToken("%%%.e30.x")).isFalse();
    }

    @Test
    void recusaCabecalhoForaDaFormaAntesDeDecodificar() {
        ControlPlaneTokenValidator v = validator(false);
        // forma do gatilho publicado do CVE-2026-114938: bloco de 'A' seguido de um '=' solto
        assertThat(v.looksLikeControlPlaneToken("A".repeat(64) + "=.e30.x")).isFalse();
        // JWS não usa padding, mesmo quando o padding seria válido em base64
        assertThat(v.looksLikeControlPlaneToken("eyJhbGciOiJFZERTQSJ9==.e30.x")).isFalse();
        // alfabeto padrão (+ /) não é base64url
        assertThat(v.looksLikeControlPlaneToken("eyJh+GciOiJFZERTQSJ9.e30.x")).isFalse();
        // comprimento que nenhum base64 produz
        assertThat(v.looksLikeControlPlaneToken("eyJhb.e30.x")).isFalse();
        // cabeçalho grande demais
        assertThat(v.looksLikeControlPlaneToken(
                "A".repeat(ControlPlaneTokenValidator.MAX_ENCODED_HEADER_LENGTH + 4) + ".e30.x")).isFalse();
        // {"alg":"EdDSA"} sem padding continua reconhecido
        assertThat(v.looksLikeControlPlaneToken("eyJhbGciOiJFZERTQSJ9.e30.x")).isTrue();
    }
}
