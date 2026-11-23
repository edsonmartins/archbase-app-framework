package br.com.archbase.oidc.provider;

import br.com.archbase.oidc.config.ArchbaseOidcProperties;
import br.com.archbase.oidc.config.ArchbaseOidcProperties.Provider;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Os provedores configurados: qual deles responde por um emissor e o {@link JwtDecoder} de cada um.
 * <p>
 * O decoder é criado no primeiro uso, não na subida: sem {@code jwks-uri} ele descobre o JWKS pelo emissor, e um IdP
 * fora do ar não pode impedir a aplicação de subir (o login local continua funcionando).
 *
 * @since 3.5.0
 */
public class OidcProviderRegistry {

    /** Códigos dos validadores (vão para o log e, resumidos, para o cliente). */
    public static final String ERROR_EXPIRED = "token_expired";
    public static final String ERROR_NOT_YET_VALID = "token_not_yet_valid";
    public static final String ERROR_NO_EXP = "missing_exp";
    public static final String ERROR_ISSUER = "invalid_issuer";
    public static final String ERROR_AUDIENCE = "invalid_audience";
    public static final String ERROR_REQUIRED_CLAIM = "missing_required_claim";

    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(5);

    private final Map<String, Provider> providers;
    private final Map<String, Pattern> issuerPatterns = new ConcurrentHashMap<>();
    private final Map<String, JwtDecoder> decoders = new ConcurrentHashMap<>();
    private final Clock clock;

    public OidcProviderRegistry(ArchbaseOidcProperties properties, Clock clock) {
        this.providers = Map.copyOf(properties.getProviders());
        this.clock = clock;
        providers.forEach((id, p) -> {
            if (p.getIssuerPattern() != null && !p.getIssuerPattern().isBlank()) {
                issuerPatterns.put(id, compile(p.getIssuerPattern()));
            }
        });
    }

    /** O provedor responsável pelo emissor, com o tenant do Archbase quando o emissor é por modelo. */
    public record Match(String providerId, Provider provider, String tenantFromIssuer) {
    }

    public Optional<Match> match(String issuer) {
        if (issuer == null) {
            return Optional.empty();
        }
        for (Map.Entry<String, Provider> e : providers.entrySet()) {
            Provider p = e.getValue();
            if (issuer.equals(p.getIssuer())) {
                return Optional.of(new Match(e.getKey(), p, null));
            }
            Pattern pattern = issuerPatterns.get(e.getKey());
            if (pattern != null) {
                Matcher m = pattern.matcher(issuer);
                if (m.matches()) {
                    // Tenant do IdP fora da lista: o emissor é deste provedor, mas o token não serve.
                    return Optional.of(new Match(e.getKey(), p, p.getAllowedTenants().get(m.group(1))));
                }
            }
        }
        return Optional.empty();
    }

    public JwtDecoder decoder(String providerId) {
        return decoders.computeIfAbsent(providerId, id -> build(id, providers.get(id)));
    }

    private JwtDecoder build(String id, Provider p) {
        SimpleClientHttpRequestFactory http = new SimpleClientHttpRequestFactory();
        http.setConnectTimeout(HTTP_TIMEOUT);
        http.setReadTimeout(HTTP_TIMEOUT);
        RestTemplate rest = new RestTemplate(http);

        NimbusJwtDecoder.JwkSetUriJwtDecoderBuilder builder = p.getJwksUri() != null && !p.getJwksUri().isBlank()
                ? NimbusJwtDecoder.withJwkSetUri(p.getJwksUri())
                : NimbusJwtDecoder.withIssuerLocation(p.getIssuer());
        NimbusJwtDecoder decoder = builder
                .restOperations(rest)
                .jwsAlgorithms(algs -> {
                    algs.clear();
                    p.getAlgorithms().forEach(a -> algs.add(SignatureAlgorithm.from(a)));
                })
                // Muitos IdPs emitem typ=JWT, outros at+jwt (RFC 9068), outros nada: não é critério aqui.
                .validateType(false)
                .build();
        decoder.setJwtValidator(validator(id, p));
        return decoder;
    }

    private OAuth2TokenValidator<Jwt> validator(String id, Provider p) {
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(this::validateTimestamps);
        validators.add(jwt -> validateIssuer(id, p, jwt));
        validators.add(jwt -> validateAudience(p, jwt));
        validators.add(jwt -> validateRequiredClaims(p, jwt));
        return new DelegatingOAuth2TokenValidator<>(validators);
    }

    // exp obrigatório: o JwtTimestampValidator do Spring aceita token sem exp, que valeria para sempre.
    private OAuth2TokenValidatorResult validateTimestamps(Jwt jwt) {
        Instant now = clock.instant();
        Duration skew = providerSkew(jwt);
        if (jwt.getExpiresAt() == null) {
            return fail(ERROR_NO_EXP, "Token sem exp");
        }
        if (now.minus(skew).isAfter(jwt.getExpiresAt())) {
            return fail(ERROR_EXPIRED, "Token expirado");
        }
        if (jwt.getNotBefore() != null && now.plus(skew).isBefore(jwt.getNotBefore())) {
            return fail(ERROR_NOT_YET_VALID, "Token ainda não é válido");
        }
        return OAuth2TokenValidatorResult.success();
    }

    private Duration providerSkew(Jwt jwt) {
        String iss = jwt.getClaimAsString("iss");
        return match(iss).map(m -> m.provider().getClockSkew()).orElse(Duration.ZERO);
    }

    private OAuth2TokenValidatorResult validateIssuer(String id, Provider p, Jwt jwt) {
        String iss = jwt.getClaimAsString("iss");
        Optional<Match> m = match(iss);
        if (m.isEmpty() || !m.get().providerId().equals(id)) {
            return fail(ERROR_ISSUER, "Emissor não pertence ao provedor " + id);
        }
        if (issuerPatterns.containsKey(id) && m.get().tenantFromIssuer() == null) {
            return fail(ERROR_ISSUER, "Tenant do IdP não permitido em allowed-tenants");
        }
        return OAuth2TokenValidatorResult.success();
    }

    private static OAuth2TokenValidatorResult validateAudience(Provider p, Jwt jwt) {
        List<String> aud = jwt.getAudience();
        if (aud != null && aud.stream().anyMatch(p.getAudiences()::contains)) {
            return OAuth2TokenValidatorResult.success();
        }
        return fail(ERROR_AUDIENCE, "Token não foi emitido para esta aplicação (aud)");
    }

    private static OAuth2TokenValidatorResult validateRequiredClaims(Provider p, Jwt jwt) {
        for (Map.Entry<String, String> required : p.getRequiredClaims().entrySet()) {
            Object value = jwt.getClaims().get(required.getKey());
            if (value == null || !required.getValue().equals(String.valueOf(value))) {
                return fail(ERROR_REQUIRED_CLAIM, "Claim exigido ausente ou diferente: " + required.getKey());
            }
        }
        return OAuth2TokenValidatorResult.success();
    }

    private static OAuth2TokenValidatorResult fail(String code, String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(code, description, null));
    }

    static Pattern compile(String issuerPattern) {
        int at = issuerPattern.indexOf("{tid}");
        String before = issuerPattern.substring(0, at);
        String after = issuerPattern.substring(at + "{tid}".length());
        return Pattern.compile(Pattern.quote(before) + "([^/]+)" + Pattern.quote(after));
    }
}
