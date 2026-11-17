package br.com.archbase.controlplane.token;

import br.com.archbase.controlplane.session.AdminRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.InvalidClaimException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.LocatorAdapter;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Valida o token do Control Plane a cada requisição, só com as chaves em memória — sem banco.
 * <p>
 * Regras: assinatura Ed25519 pela chave do {@code kid}; {@code iss} igual ao configurado; {@code aud} contém a URL
 * pública da instância; {@code exp} presente e não vencido; {@code sub} presente. No token de sessão, {@code tnt} é
 * obrigatório e, se o produto exigir, {@code seat} também.
 *
 * @since 3.4.6
 */
public class ControlPlaneTokenValidator {

    private static final Logger log = LoggerFactory.getLogger(ControlPlaneTokenValidator.class);

    /** {@code alg} de um JWS assinado com Ed25519. O JWT próprio do Archbase é HS256, então não há confusão. */
    private static final Pattern ED_DSA_ALG = Pattern.compile("\"alg\"\\s*:\\s*\"(EdDSA|Ed25519)\"");

    private final JwksKeyStore keyStore;
    private final String issuer;
    private final String audience;
    private final Duration clockSkew;
    private final boolean seatRequired;

    public ControlPlaneTokenValidator(JwksKeyStore keyStore, String issuer, String audience, Duration clockSkew,
                                      boolean seatRequired) {
        this.keyStore = keyStore;
        this.issuer = issuer;
        this.audience = audience;
        this.clockSkew = clockSkew;
        this.seatRequired = seatRequired;
    }

    /**
     * Se o token tem a forma de um token do Control Plane (JWS com {@code alg} EdDSA). Não valida nada além disso.
     */
    public boolean looksLikeControlPlaneToken(String token) {
        if (token == null) {
            return false;
        }
        int dot = token.indexOf('.');
        if (dot <= 0 || token.indexOf('.', dot + 1) < 0) {
            return false;
        }
        try {
            String header = new String(Base64.getUrlDecoder().decode(token.substring(0, dot)), StandardCharsets.UTF_8);
            return ED_DSA_ALG.matcher(header).find();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public TokenValidationResult validate(String token) {
        Jws<Claims> jws;
        try {
            jws = Jwts.parser()
                    .keyLocator(new KeyLocator())
                    .requireIssuer(issuer)
                    .requireAudience(audience)
                    .clockSkewSeconds(clockSkew.toSeconds())
                    .build()
                    .parseSignedClaims(token);
        } catch (UnknownKeyIdException e) {
            log.warn("controlplane.token.rejected code={} kid={}", TokenValidationResult.UNKNOWN_KEY, e.getMessage());
            return TokenValidationResult.rejected(401, TokenValidationResult.UNKNOWN_KEY,
                    "Chave de assinatura desconhecida");
        } catch (ExpiredJwtException e) {
            log.debug("controlplane.token.rejected code={}", TokenValidationResult.TOKEN_EXPIRED);
            return TokenValidationResult.rejected(401, TokenValidationResult.TOKEN_EXPIRED, "Token vencido");
        } catch (InvalidClaimException e) {
            String claim = e.getClaimName();
            if ("aud".equals(claim)) {
                log.warn("controlplane.token.rejected code={}", TokenValidationResult.WRONG_AUDIENCE);
                return TokenValidationResult.rejected(401, TokenValidationResult.WRONG_AUDIENCE,
                        "Token emitido para outra instância");
            }
            if ("iss".equals(claim)) {
                log.warn("controlplane.token.rejected code={}", TokenValidationResult.WRONG_ISSUER);
                return TokenValidationResult.rejected(401, TokenValidationResult.WRONG_ISSUER,
                        "Token de outro emissor");
            }
            return invalid("claim " + claim + " inválido");
        } catch (SignatureException e) {
            log.warn("controlplane.token.rejected code={}", TokenValidationResult.INVALID_SIGNATURE);
            return TokenValidationResult.rejected(401, TokenValidationResult.INVALID_SIGNATURE,
                    "Assinatura do token inválida");
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("controlplane.token.rejected code={} erro={}", TokenValidationResult.INVALID_TOKEN, e.getMessage());
            return TokenValidationResult.rejected(401, TokenValidationResult.INVALID_TOKEN, "Token inválido");
        }
        return checkClaims(jws);
    }

    private TokenValidationResult checkClaims(Jws<Claims> jws) {
        Claims claims = jws.getPayload();
        if (claims.getExpiration() == null) {
            return invalid("token sem exp");
        }
        if (isBlank(claims.getSubject())) {
            return invalid("token sem sub");
        }
        ControlPlaneClaims.TokenUse use;
        String tokenUse = claims.get("token_use", String.class);
        if (tokenUse == null || "session".equals(tokenUse)) {
            use = ControlPlaneClaims.TokenUse.SESSION;
        } else if ("instance".equals(tokenUse)) {
            use = ControlPlaneClaims.TokenUse.INSTANCE;
        } else {
            return invalid("token_use desconhecido: " + tokenUse);
        }

        String tenant = claims.get("tnt", String.class);
        String seat = claims.get("seat", String.class);
        AdminRole role = null;
        if (use == ControlPlaneClaims.TokenUse.SESSION) {
            if (isBlank(tenant)) {
                log.warn("controlplane.token.rejected code={} sub={}", TokenValidationResult.TENANT_REQUIRED,
                        claims.getSubject());
                // 403, como a recusa do filtro para token de instância sem X-TENANT-ID: um código, um
                // status. 401 faria o cliente tentar renovar a sessão, o que não resolve.
                return TokenValidationResult.rejected(403, TokenValidationResult.TENANT_REQUIRED,
                        "Token de sessão sem tenant");
            }
            role = AdminRole.fromClaim(claims.get("adm", String.class));
            if (role == null) {
                return invalid("adm desconhecido: " + claims.get("adm", String.class));
            }
            if (seatRequired && isBlank(seat)) {
                log.info("controlplane.token.rejected code={} sub={} tnt={}", TokenValidationResult.NO_SEAT,
                        claims.getSubject(), tenant);
                return TokenValidationResult.rejected(403, TokenValidationResult.NO_SEAT,
                        "Usuário sem assento neste produto");
            }
        }

        return TokenValidationResult.valid(new ControlPlaneClaims(
                use,
                claims.getSubject(),
                claims.get("email", String.class),
                isBlank(tenant) ? null : tenant,
                role,
                isBlank(seat) ? null : seat,
                claims.getId(),
                jws.getHeader().getKeyId(),
                claims.getIssuedAt() == null ? null : claims.getIssuedAt().toInstant(),
                claims.getExpiration().toInstant()));
    }

    private static TokenValidationResult invalid(String detail) {
        log.warn("controlplane.token.rejected code={} detalhe={}", TokenValidationResult.INVALID_TOKEN, detail);
        return TokenValidationResult.rejected(401, TokenValidationResult.INVALID_TOKEN, "Token inválido");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private final class KeyLocator extends LocatorAdapter<Key> {
        @Override
        protected Key locate(JwsHeader header) {
            String kid = header.getKeyId();
            return keyStore.resolve(kid).orElseThrow(() -> new UnknownKeyIdException(kid));
        }
    }

    /** {@code kid} ausente ou sem chave correspondente, mesmo depois de consultar o JWKS. */
    static final class UnknownKeyIdException extends JwtException {
        UnknownKeyIdException(String kid) {
            super(String.valueOf(kid));
        }
    }
}
