package br.com.archbase.controlplane.command;

import br.com.archbase.controlplane.token.JwksKeyStore;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.LocatorAdapter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;

/**
 * Verifica a assinatura de um comando antes de qualquer efeito.
 * <p>
 * <b>Contrato assumido — confirmar com a ADR-0014.</b> A mensagem MQTT é um JWS compacto assinado pelo Control Plane
 * com Ed25519 (mesmo JWKS do token de sessão), com os claims {@code iss} (Control Plane), {@code sub}
 * ({@code instance-id} desta instância), {@code token_use=command}, {@code jti} (id do comando), {@code cmd} (tipo),
 * {@code iat} e o objeto {@code data}. {@code exp}, se vier, é respeitado. Só esta classe conhece o formato.
 *
 * @since 3.4.6
 */
public class CommandVerifier {

    private final JwksKeyStore keyStore;
    private final String issuer;
    private final String instanceId;
    private final JsonMapper json = JsonMapper.builder().build();

    public CommandVerifier(JwksKeyStore keyStore, String issuer, String instanceId) {
        this.keyStore = keyStore;
        this.issuer = issuer;
        this.instanceId = instanceId;
    }

    public ControlPlaneCommand verify(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new CommandRejectedException("mensagem vazia");
        }
        String token = payload.trim();
        Claims claims;
        try {
            claims = Jwts.parser()
                    .keyLocator(new LocatorAdapter<Key>() {
                        @Override
                        protected Key locate(JwsHeader header) {
                            return keyStore.resolve(header.getKeyId()).orElseThrow(
                                    () -> new CommandRejectedException("chave desconhecida: " + header.getKeyId()));
                        }
                    })
                    .requireIssuer(issuer)
                    .requireSubject(instanceId)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (CommandRejectedException e) {
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            throw new CommandRejectedException("assinatura ou claims inválidos: " + e.getMessage(), e);
        }
        if (!"command".equals(claims.get("token_use", String.class))) {
            throw new CommandRejectedException("o documento não é um comando (token_use)");
        }
        String id = claims.getId();
        String type = claims.get("cmd", String.class);
        if (id == null || id.isBlank() || type == null || type.isBlank()) {
            throw new CommandRejectedException("comando sem jti ou cmd");
        }
        // O corpo é relido pelo Jackson: o desserializador do jjwt converte números para Double.
        JsonNode data = payloadJson(token).path("data");
        return new ControlPlaneCommand(id, type,
                claims.getIssuedAt() == null ? null : claims.getIssuedAt().toInstant(),
                data.isMissingNode() ? json.createObjectNode() : data);
    }

    /** Id do comando sem verificar a assinatura — só para correlacionar o aviso de recusa. */
    public String unverifiedId(String payload) {
        try {
            return payloadJson(payload.trim()).path("jti").asString(null);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private JsonNode payloadJson(String token) {
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            throw new CommandRejectedException("formato inválido");
        }
        return json.readTree(new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8));
    }
}
