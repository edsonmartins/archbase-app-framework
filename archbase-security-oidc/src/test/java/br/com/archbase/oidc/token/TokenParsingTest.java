package br.com.archbase.oidc.token;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Leitura de claims e do emissor sem verificar")
class TokenParsingTest {

    private static String jws(String payloadJson) {
        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
        return enc.encodeToString("{\"alg\":\"RS256\"}".getBytes(StandardCharsets.UTF_8)) + "."
                + enc.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8)) + ".assinatura";
    }

    @Test
    @DisplayName("ClaimReader: nome exato (inclusive URL com pontos) vence o caminho")
    void nomeVenceCaminho() {
        Map<String, Object> claims = Map.of(
                "https://empresa.com/roles", List.of("admin"),
                "realm_access", Map.of("roles", List.of("user")),
                "a.b", "nome-com-ponto",
                "a", Map.of("b", "caminho"));

        assertThat(ClaimReader.read(claims, "https://empresa.com/roles")).isEqualTo(List.of("admin"));
        assertThat(ClaimReader.read(claims, "realm_access.roles")).isEqualTo(List.of("user"));
        assertThat(ClaimReader.read(claims, "a.b")).isEqualTo("nome-com-ponto");
        assertThat(ClaimReader.read(claims, "nao.existe")).isNull();
    }

    @Test
    @DisplayName("ClaimReader: texto, número e booleano")
    void tipos() {
        Map<String, Object> claims = Map.of("s", "x", "n", 42, "vazio", " ", "lista", List.of("a"),
                "v1", true, "v2", "true", "v3", "sim");
        assertThat(ClaimReader.readString(claims, "s")).isEqualTo("x");
        assertThat(ClaimReader.readString(claims, "n")).isEqualTo("42");
        assertThat(ClaimReader.readString(claims, "vazio")).isNull();
        assertThat(ClaimReader.readString(claims, "lista")).isNull();
        assertThat(ClaimReader.readTrue(claims, "v1")).isTrue();
        assertThat(ClaimReader.readTrue(claims, "v2")).isTrue();
        assertThat(ClaimReader.readTrue(claims, "v3")).isFalse();
        assertThat(ClaimReader.readTrue(claims, "ausente")).isFalse();
    }

    @Test
    @DisplayName("UnverifiedJwt: lê o iss de um JWS bem formado")
    void leIssuer() {
        assertThat(UnverifiedJwt.issuer(jws("{\"iss\":\"https://idp.example.com\",\"sub\":\"x\"}")))
                .contains("https://idp.example.com");
    }

    @Test
    @DisplayName("UnverifiedJwt: forma inválida devolve vazio, sem decodificar (CVE-2026-114938)")
    void formaInvalida() {
        String ok = jws("{\"iss\":\"https://idp.example.com\"}");
        String[] partes = ok.split("\\.");

        assertThat(UnverifiedJwt.issuer(null)).isEmpty();
        assertThat(UnverifiedJwt.issuer("nao-e-jwt")).isEmpty();
        assertThat(UnverifiedJwt.issuer(partes[0] + "." + partes[1])).isEmpty();                  // 2 partes
        assertThat(UnverifiedJwt.issuer(ok + ".a.b")).isEmpty();                                    // 5 partes (JWE)
        assertThat(UnverifiedJwt.issuer(partes[0] + "." + "A".repeat(64) + "=" + ".x")).isEmpty(); // padding
        assertThat(UnverifiedJwt.issuer(partes[0] + "." + partes[1] + "+/" + ".x")).isEmpty();      // alfabeto
        assertThat(UnverifiedJwt.issuer(partes[0] + "." + "A".repeat(UnverifiedJwt.MAX_TOKEN_LENGTH) + ".x"))
                .isEmpty();                                                                          // tamanho
        assertThat(UnverifiedJwt.issuer(jws("[1,2]"))).isEmpty();                                   // não é objeto
        assertThat(UnverifiedJwt.issuer(jws("{\"iss\":42}"))).isEmpty();                           // iss não texto
        assertThat(UnverifiedJwt.issuer(jws("{\"sub\":\"x\"}"))).isEmpty();                          // sem iss
    }
}
