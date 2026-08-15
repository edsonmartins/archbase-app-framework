package br.com.archbase.analytics;

import br.com.archbase.analytics.token.CubeTokenMinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * O token cunhado precisa validar a assinatura com o mesmo segredo do Cube e
 * carregar as claims de escopo que o produto forneceu, verbatim.
 */
class CubeTokenMinterTest {

    private static final String SECRET = "archbase-analytics-secret-de-teste-32bytes+";
    private static final Instant NOW = Instant.parse("2026-08-15T12:00:00Z");
    private final ObjectMapper mapper = new ObjectMapper();
    private final CubeTokenMinter minter = new CubeTokenMinter(mapper, SECRET, 120);

    @Test
    void assinaturaValidaComOSegredo() throws Exception {
        String token = minter.mint("u@x.com", Map.of(), NOW);
        String[] p = token.split("\\.");
        assertEquals(3, p.length);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] expected = mac.doFinal((p[0] + "." + p[1]).getBytes(StandardCharsets.UTF_8));
        assertArrayEquals(expected, Base64.getUrlDecoder().decode(p[2]));
    }

    @Test
    void copiaAsClaimsDeEscopoVerbatim() {
        Map<String, Object> c = claims(minter.mint("u",
                Map.of("escopo", "filial", "filial_ids", java.util.List.of(3, 7)), NOW));
        assertEquals("filial", c.get("escopo"));
        assertEquals(java.util.List.of(3, 7), c.get("filial_ids"));
        assertEquals("u", c.get("sub"));
    }

    @Test
    void escopoVazioNaoLevaClaims() {
        Map<String, Object> c = claims(minter.mint("u", Map.of(), NOW));
        assertFalse(c.containsKey("escopo"));
        assertEquals(NOW.plusSeconds(120).getEpochSecond(), ((Number) c.get("exp")).longValue());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> claims(String token) {
        try {
            return mapper.readValue(Base64.getUrlDecoder().decode(token.split("\\.")[1]), Map.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
