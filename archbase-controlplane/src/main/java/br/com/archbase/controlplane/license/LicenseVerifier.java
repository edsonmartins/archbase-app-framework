package br.com.archbase.controlplane.license;

import br.com.archbase.controlplane.token.JwksKeyStore;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.LocatorAdapter;

import java.security.Key;

/**
 * Verifica a licença da instância offline, com as mesmas chaves do token de sessão.
 * <p>
 * <b>Contrato assumido — confirmar com a ADR-0005.</b> A licença é um JWS compacto assinado pelo Control Plane com
 * Ed25519, com os claims:
 * <ul>
 * <li>{@code iss}: o Control Plane configurado;</li>
 * <li>{@code sub}: o {@code instance-id} da instância;</li>
 * <li>{@code prd}: o código do produto;</li>
 * <li>{@code token_use}: {@code license};</li>
 * <li>{@code iat} e {@code exp}.</li>
 * </ul>
 * Se a ADR definir outro formato, só esta classe muda.
 * <p>
 * Licença vencida é aceita aqui — a assinatura é verificada antes da validade, e quem decide entre {@code GRACE} e
 * {@code EXPIRED} é o {@link InstanceLicense#statusAt}. O que se recusa é licença forjada ou de outra instância.
 *
 * @since 3.4.6
 */
public class LicenseVerifier {

    private final JwksKeyStore keyStore;
    private final String issuer;
    private final String instanceId;
    private final String product;

    public LicenseVerifier(JwksKeyStore keyStore, String issuer, String instanceId, String product) {
        this.keyStore = keyStore;
        this.issuer = issuer;
        this.instanceId = instanceId;
        this.product = product;
    }

    public InstanceLicense verify(String token) {
        if (instanceId == null || instanceId.isBlank()) {
            throw new LicenseRejectedException("archbase.controlplane.instance-id não definido: sem ele não há como "
                    + "saber se a licença é desta instância");
        }
        Claims claims;
        try {
            Jws<Claims> jws = Jwts.parser()
                    .keyLocator(new LocatorAdapter<Key>() {
                        @Override
                        protected Key locate(JwsHeader header) {
                            return keyStore.resolve(header.getKeyId()).orElseThrow(
                                    () -> new LicenseRejectedException("chave desconhecida: " + header.getKeyId()));
                        }
                    })
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token);
            claims = jws.getPayload();
        } catch (ExpiredJwtException e) {
            // Assinatura e emissor já conferidos: a validade é avaliada por quem consulta o status.
            claims = e.getClaims();
            if (!issuer.equals(claims.getIssuer())) {
                throw new LicenseRejectedException("licença de outro emissor");
            }
        } catch (LicenseRejectedException e) {
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            throw new LicenseRejectedException("licença inválida: " + e.getMessage(), e);
        }

        if (!"license".equals(claims.get("token_use", String.class))) {
            throw new LicenseRejectedException("o documento não é uma licença (token_use)");
        }
        if (!instanceId.equals(claims.getSubject())) {
            throw new LicenseRejectedException("licença de outra instância: " + claims.getSubject());
        }
        String licensedProduct = claims.get("prd", String.class);
        if (!product.equals(licensedProduct)) {
            throw new LicenseRejectedException("licença de outro produto: " + licensedProduct);
        }
        if (claims.getExpiration() == null) {
            throw new LicenseRejectedException("licença sem exp");
        }
        return new InstanceLicense(instanceId, licensedProduct,
                claims.getIssuedAt() == null ? null : claims.getIssuedAt().toInstant(),
                claims.getExpiration().toInstant(), token);
    }
}
