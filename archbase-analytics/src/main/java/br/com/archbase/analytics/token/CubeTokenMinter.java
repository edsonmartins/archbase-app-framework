package br.com.archbase.analytics.token;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

/**
 * Cunha o token curto que o proxy envia ao Cube.
 *
 * <p>HS256 assinado com o segredo compartilhado — o mesmo que o Cube valida. O
 * contexto é assinado pelo servidor e não é forjável pelo cliente; o token do
 * app nunca chega ao Cube.
 *
 * <p>HMAC-SHA256 próprio (não jjwt): o formato é exatamente o que o Cube espera
 * e não depende da versão de nenhuma lib de JWT. O segredo é usado como bytes
 * UTF-8, idêntico ao que o Cube faz com a env var.
 *
 * <p>As claims de escopo são fornecidas pelo produto via
 * {@link br.com.archbase.analytics.port.DataScopeProvider} — o minter não sabe
 * o que significam.
 */
public final class CubeTokenMinter {

    private static final Base64.Encoder B64URL = Base64.getUrlEncoder().withoutPadding();
    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final int ttlSeconds;

    public CubeTokenMinter(ObjectMapper objectMapper, String secret, int ttlSeconds) {
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttlSeconds = ttlSeconds;
    }

    /**
     * Token para {@code subject} com as claims de escopo. As claims chegam do
     * produto e são copiadas verbatim (o minter só acrescenta sub/iat/exp).
     *
     * @param now instante base (parâmetro para ser testável — nunca lê o relógio)
     */
    public String mint(String subject, Map<String, Object> scopeClaims, Instant now) {
        ObjectNode claims = objectMapper.valueToTree(
                scopeClaims != null ? scopeClaims : Map.of());
        claims.put("sub", subject != null ? subject : "");
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", now.plusSeconds(ttlSeconds).getEpochSecond());

        String header = B64URL.encodeToString(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
        String body = B64URL.encodeToString(claims.toString().getBytes(StandardCharsets.UTF_8));
        String signed = header + "." + body;
        String signature = B64URL.encodeToString(hmacSha256(signed));
        return signed + "." + signature;
    }

    private byte[] hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("JVM sem HmacSHA256", e);
        }
    }
}
