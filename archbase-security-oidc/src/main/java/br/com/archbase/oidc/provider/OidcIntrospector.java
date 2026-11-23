package br.com.archbase.oidc.provider;

import br.com.archbase.oidc.config.ArchbaseOidcProperties;
import br.com.archbase.oidc.config.ArchbaseOidcProperties.Introspection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pergunta ao IdP se um token ainda está ativo (RFC 7662), com cache.
 * <p>
 * A chave do cache é o SHA-256 do token — o token em claro não fica guardado na memória do processo além da
 * requisição. A resposta vale {@code cache-ttl}, nunca além do {@code exp} do próprio token; respostas negativas
 * também ficam em cache, para um token revogado não martelar o IdP. O cache é limitado (LRU).
 *
 * @since 3.5.0
 */
public class OidcIntrospector {

    /** Resultado de uma consulta. */
    public enum Status { ACTIVE, INACTIVE, UNAVAILABLE }

    static final int MAX_ENTRIES = 10_000;
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(3);
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Logger log = LoggerFactory.getLogger(OidcIntrospector.class);

    private record Cached(boolean active, Instant expiresAt) {
    }

    private final ArchbaseOidcProperties properties;
    private final Clock clock;
    private final RestTemplate rest;
    private final Map<String, Cached> cache = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Cached> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    public OidcIntrospector(ArchbaseOidcProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        SimpleClientHttpRequestFactory http = new SimpleClientHttpRequestFactory();
        http.setConnectTimeout(HTTP_TIMEOUT);
        http.setReadTimeout(HTTP_TIMEOUT);
        this.rest = new RestTemplate(http);
    }

    public boolean enabled(String providerId) {
        ArchbaseOidcProperties.Provider p = properties.getProviders().get(providerId);
        return p != null && p.getIntrospection().isEnabled();
    }

    /**
     * @param tokenExpiresAt {@code exp} do token já validado, para limitar o tempo de cache
     */
    public Status check(String providerId, String token, Instant tokenExpiresAt) {
        Introspection config = properties.getProviders().get(providerId).getIntrospection();
        String key = providerId + ":" + sha256(token);
        Instant now = clock.instant();
        synchronized (cache) {
            Cached cached = cache.get(key);
            if (cached != null && now.isBefore(cached.expiresAt())) {
                return cached.active() ? Status.ACTIVE : Status.INACTIVE;
            }
        }

        Boolean active = ask(providerId, config, token);
        if (active == null) {
            return Status.UNAVAILABLE; // não vai para o cache: na próxima, tenta de novo
        }
        Instant until = now.plus(config.getCacheTtl());
        if (tokenExpiresAt != null && tokenExpiresAt.isBefore(until)) {
            until = tokenExpiresAt;
        }
        synchronized (cache) {
            cache.put(key, new Cached(active, until));
        }
        return active ? Status.ACTIVE : Status.INACTIVE;
    }

    /** {@code null} = IdP não respondeu de forma utilizável. */
    private Boolean ask(String providerId, Introspection config, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString(
                (config.getClientId() + ":" + config.getClientSecret()).getBytes(StandardCharsets.UTF_8)));
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("token", token);
        form.add("token_type_hint", "access_token");
        try {
            String body = rest.postForObject(config.getUri(), new HttpEntity<>(form, headers), String.class);
            JsonNode node = body == null ? null : JSON.readTree(body);
            JsonNode active = node == null ? null : node.get("active");
            if (active == null || !active.isBoolean()) {
                log.warn("oidc.introspection.bad_response provider={}", providerId);
                return null;
            }
            return active.asBoolean();
        } catch (RestClientException | tools.jackson.core.JacksonException e) {
            log.warn("oidc.introspection.unavailable provider={} cause={}", providerId, e.getMessage());
            return null;
        }
    }

    void clearCache() {
        synchronized (cache) {
            cache.clear();
        }
    }

    private static String sha256(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
