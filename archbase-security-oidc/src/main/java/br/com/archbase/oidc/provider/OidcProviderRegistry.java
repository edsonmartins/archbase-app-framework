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
    public static final String ERROR_LOGOUT_TOKEN = "invalid_logout_token";

    /** Membro de {@code events} que identifica um logout token (Back-Channel Logout 1.0, §2.4). */
    public static final String BACKCHANNEL_LOGOUT_EVENT = "http://schemas.openid.net/event/backchannel-logout";

    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(5);

    private final Map<String, Provider> providers;
    private final Map<String, Pattern> issuerPatterns = new ConcurrentHashMap<>();
    private final Map<String, JwtDecoder> decoders = new ConcurrentHashMap<>();
    private final Map<String, JwtDecoder> logoutDecoders = new ConcurrentHashMap<>();
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

    /**
     * Decoder do {@code logout_token} do Back-Channel Logout: mesmas chaves, algoritmos e emissor do access token,
     * mas o {@code aud} é o de {@code logout.backchannel.audiences}, os {@code required-claims} não se aplicam (são do
     * access token) e as regras de tempo e de conteúdo são as da especificação (§2.6).
     */
    public JwtDecoder logoutDecoder(String providerId) {
        return logoutDecoders.computeIfAbsent(providerId, id -> {
            Provider p = providers.get(id);
            NimbusJwtDecoder decoder = nimbus(p);
            List<String> audiences = p.getLogout().getBackchannel().getAudiences() == null
                    || p.getLogout().getBackchannel().getAudiences().isEmpty()
                    ? p.getAudiences() : p.getLogout().getBackchannel().getAudiences();
            decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(List.of(
                    jwt -> validateIssuer(id, p, jwt),
                    jwt -> validateAudience(audiences, jwt),
                    jwt -> validateLogoutClaims(p, jwt))));
            return decoder;
        });
    }

    private OAuth2TokenValidatorResult validateLogoutClaims(Provider p, Jwt jwt) {
        Instant now = clock.instant();
        Duration skew = p.getClockSkew();
        Instant iat = jwt.getIssuedAt();
        if (iat == null) {
            return fail(ERROR_LOGOUT_TOKEN, "logout_token sem iat");
        }
        if (iat.isAfter(now.plus(skew))) {
            return fail(ERROR_LOGOUT_TOKEN, "logout_token emitido no futuro");
        }
        if (iat.isBefore(now.minus(p.getLogout().getBackchannel().getMaxAge()).minus(skew))) {
            return fail(ERROR_LOGOUT_TOKEN, "logout_token velho demais (iat)");
        }
        // exp é obrigatório na versão final da especificação, mas IdPs anteriores não o emitem; o iat já limita.
        if (jwt.getExpiresAt() != null && now.minus(skew).isAfter(jwt.getExpiresAt())) {
            return fail(ERROR_LOGOUT_TOKEN, "logout_token expirado");
        }
        Object events = jwt.getClaims().get("events");
        if (!(events instanceof Map<?, ?> map) || !(map.get(BACKCHANNEL_LOGOUT_EVENT) instanceof Map<?, ?>)) {
            return fail(ERROR_LOGOUT_TOKEN, "logout_token sem o evento de back-channel logout");
        }
        // nonce proibido: é o que impede um ID token de ser usado como aviso de logout.
        if (jwt.getClaims().containsKey("nonce")) {
            return fail(ERROR_LOGOUT_TOKEN, "logout_token não pode ter nonce");
        }
        if (blankClaim(jwt, "sub") && blankClaim(jwt, "sid")) {
            return fail(ERROR_LOGOUT_TOKEN, "logout_token sem sub nem sid");
        }
        return OAuth2TokenValidatorResult.success();
    }

    private static boolean blankClaim(Jwt jwt, String name) {
        String v = jwt.getClaimAsString(name);
        return v == null || v.isBlank();
    }

    private JwtDecoder build(String id, Provider p) {
        NimbusJwtDecoder decoder = nimbus(p);
        decoder.setJwtValidator(validator(id, p));
        return decoder;
    }

    private static NimbusJwtDecoder nimbus(Provider p) {
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
        return decoder;
    }

    private OAuth2TokenValidator<Jwt> validator(String id, Provider p) {
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(this::validateTimestamps);
        validators.add(jwt -> validateIssuer(id, p, jwt));
        validators.add(jwt -> validateAudience(p.getAudiences(), jwt));
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

    private static OAuth2TokenValidatorResult validateAudience(List<String> accepted, Jwt jwt) {
        List<String> aud = jwt.getAudience();
        if (aud != null && aud.stream().anyMatch(accepted::contains)) {
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
