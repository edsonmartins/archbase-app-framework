package br.com.archbase.analytics.proxy;

import br.com.archbase.analytics.port.AnalyticsAuditPort;
import br.com.archbase.analytics.port.AnalyticsAuditPort.AuditedQuery;
import br.com.archbase.analytics.port.DataScopeProvider;
import br.com.archbase.analytics.token.CubeTokenMinter;
import br.com.archbase.analytics.util.Hashes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * Passthrough auditado do Cube — o único caminho até ele; o Cube nunca é
 * exposto ao navegador. Responsabilidades: limites (timeout, teto de linhas,
 * concorrência/usuário), auditoria de toda consulta, e tradução de falha para
 * o conjunto FECHADO de códigos que a biblioteca de frontend trata.
 *
 * <p>Genérico e sem domínio: as claims de escopo do token cunhado vêm do
 * {@link DataScopeProvider} do produto. Mensagem textual do Cube nunca chega ao
 * cliente. Sucesso devolve o corpo nativo do Cube inalterado; truncamento é só
 * header.
 *
 * <p>Streaming (repasse sem bufferização): pendente; o teto de linhas mantém o
 * corpo limitado.
 */
@RestController
@RequestMapping("${archbase.analytics.base-path:/api/analytics}/v1")
public class AnalyticsProxyController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsProxyController.class);

    private final AnalyticsAuditPort audit;
    private final DataScopeProvider scopeProvider;
    private final CubeTokenMinter minter;
    private final ObjectMapper objectMapper;
    private final AnalyticsProperties props;
    private final HttpClient http;

    private final ConcurrentHashMap<String, Semaphore> semaphores = new ConcurrentHashMap<>();

    public AnalyticsProxyController(AnalyticsAuditPort audit, DataScopeProvider scopeProvider,
                                    CubeTokenMinter minter, ObjectMapper objectMapper,
                                    AnalyticsProperties props) {
        this.audit = audit;
        this.scopeProvider = scopeProvider;
        this.minter = minter;
        this.objectMapper = objectMapper;
        this.props = props;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    /** Token cunhado a partir da sessão, com o escopo projetado pelo produto. */
    private String cubeToken() {
        String user = currentUser();
        Map<String, Object> claims = scopeProvider.claimsForUser(user);
        return "Bearer " + minter.mint(user, claims, Instant.now());
    }

    // ── /v1/meta ────────────────────────────────────────────────────────────

    @GetMapping("/meta")
    public ResponseEntity<byte[]> meta() {
        try {
            HttpResponse<byte[]> resp = http.send(
                    request("/v1/meta", cubeToken(), null, props.getTimeoutSeconds()),
                    HttpResponse.BodyHandlers.ofByteArray());
            return resp.statusCode() == 200 ? json(HttpStatus.OK, resp.body()) : error(resp.statusCode());
        } catch (java.net.http.HttpTimeoutException e) {
            return envelope(HttpStatus.GATEWAY_TIMEOUT, "QUERY_TIMEOUT", false);
        } catch (Exception e) {
            log.warn("Analytics: falha no repasse de /meta ao Cube.", e);
            return envelope(HttpStatus.BAD_GATEWAY, "UPSTREAM_ERROR", null);
        }
    }

    // ── /v1/load ──────────────────────────────────────────────────────────────

    @PostMapping("/load")
    public ResponseEntity<byte[]> load(
            @RequestBody String body,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Analytics-Origin", required = false) String origin) {
        return runLoad(body, authorization, validOrigin(origin));
    }

    @GetMapping("/load")
    public ResponseEntity<byte[]> loadGet(
            @RequestParam("query") String query,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Analytics-Origin", required = false) String origin) {
        return runLoad("{\"query\":" + query + "}", authorization, validOrigin(origin));
    }

    private ResponseEntity<byte[]> runLoad(String body, String authorization, String origin) {
        Instant start = Instant.now();
        String user = currentUser();
        String ctxHash = securityContextHash(authorization);

        ObjectNode envelope;
        ObjectNode query;
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.has("query") && root.get("query").isObject()) {
                envelope = (ObjectNode) root;
                query = (ObjectNode) root.get("query");
            } else if (root.isObject()) {
                envelope = objectMapper.createObjectNode();
                envelope.set("query", root);
                query = (ObjectNode) root;
            } else {
                return envelope(HttpStatus.BAD_GATEWAY, "UPSTREAM_ERROR", null);
            }
        } catch (Exception e) {
            return envelope(HttpStatus.BAD_GATEWAY, "UPSTREAM_ERROR", null);
        }

        int requested = query.path("limit").asInt(props.getRowLimit());
        int effective = Math.min(Math.max(requested, 1), props.getRowLimit());
        query.put("limit", effective);
        String queryJson = query.toString();

        Semaphore sem = semaphores.computeIfAbsent(user, u -> new Semaphore(props.getConcurrencyPerUser()));
        if (!sem.tryAcquire()) {
            record(user, ctxHash, queryJson, origin, start, null, null, false, "erro", "CONCURRENCY_LIMIT");
            return envelope(HttpStatus.TOO_MANY_REQUESTS, "CONCURRENCY_LIMIT", true);
        }
        try {
            return queryCube(envelope, cubeToken(), user, ctxHash, queryJson, origin, start, effective);
        } finally {
            sem.release();
        }
    }

    private ResponseEntity<byte[]> queryCube(ObjectNode envelope, String cubeAuth, String user,
                                             String ctxHash, String queryJson, String origin,
                                             Instant start, int effectiveLimit) {
        Instant deadline = start.plusSeconds(props.getTimeoutSeconds());
        try {
            while (true) {
                long remaining = Duration.between(Instant.now(), deadline).toSeconds();
                if (remaining < 1) {
                    record(user, ctxHash, queryJson, origin, start, durationMs(start), null, false, "erro", "QUERY_TIMEOUT");
                    return envelope(HttpStatus.GATEWAY_TIMEOUT, "QUERY_TIMEOUT", false);
                }
                HttpResponse<byte[]> resp = http.send(
                        request("/v1/load", cubeAuth, envelope.toString(), (int) remaining),
                        HttpResponse.BodyHandlers.ofByteArray());

                if (resp.statusCode() != 200) {
                    String code = resp.statusCode() == 403 ? "FORBIDDEN_MEMBER" : "UPSTREAM_ERROR";
                    record(user, ctxHash, queryJson, origin, start, durationMs(start), null, false, "erro", code);
                    return error(resp.statusCode());
                }
                JsonNode payload = objectMapper.readTree(resp.body());
                if ("Continue wait".equals(payload.path("error").asText(null))) {
                    continue;
                }
                int rows = countRows(payload);
                boolean truncated = rows >= effectiveLimit;
                record(user, ctxHash, queryJson, origin, start, durationMs(start), rows, truncated, "ok", null);

                var builder = ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON);
                if (truncated) {
                    builder.header("X-Analytics-Truncated", "true")
                           .header("X-Analytics-Row-Limit", String.valueOf(props.getRowLimit()));
                }
                return builder.body(resp.body());
            }
        } catch (java.net.http.HttpTimeoutException e) {
            record(user, ctxHash, queryJson, origin, start, durationMs(start), null, false, "erro", "QUERY_TIMEOUT");
            return envelope(HttpStatus.GATEWAY_TIMEOUT, "QUERY_TIMEOUT", false);
        } catch (Exception e) {
            log.warn("Analytics: falha no repasse ao Cube.", e);
            record(user, ctxHash, queryJson, origin, start, durationMs(start), null, false, "erro", "UPSTREAM_ERROR");
            return envelope(HttpStatus.BAD_GATEWAY, "UPSTREAM_ERROR", null);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════

    private HttpRequest request(String path, String authorization, String body, int timeoutSeconds) {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(props.getCubeUrl() + path))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Content-Type", "application/json");
        if (authorization != null && !authorization.isBlank()) {
            b.header("Authorization", authorization);
        }
        return body == null ? b.GET().build()
                : b.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
    }

    private static int countRows(JsonNode payload) {
        if (payload.has("results") && payload.get("results").isArray()) {
            int total = 0;
            for (JsonNode r : payload.get("results")) {
                total += r.path("data").size();
            }
            return total;
        }
        return payload.path("data").size();
    }

    private static String currentUser() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getName() != null ? a.getName() : "desconhecido";
    }

    private static String securityContextHash(String authorization) {
        if (authorization == null) {
            return Hashes.sha256("sem-token");
        }
        String[] parts = authorization.replaceFirst("(?i)^Bearer\\s+", "").split("\\.");
        return Hashes.sha256(parts.length >= 2 ? parts[1] : authorization);
    }

    private void record(String user, String ctxHash, String queryJson, String origin,
                        Instant start, Integer durationMs, Integer rows, boolean truncated,
                        String outcome, String errorCode) {
        audit.record(new AuditedQuery(user, ctxHash, queryJson, origin, "load",
                start, durationMs, rows, truncated, outcome, errorCode));
    }

    private static Integer durationMs(Instant start) {
        return (int) Duration.between(start, Instant.now()).toMillis();
    }

    private static String validOrigin(String origin) {
        return "widget".equals(origin) ? "widget" : "explorer";
    }

    private ResponseEntity<byte[]> error(int upstreamStatus) {
        return switch (upstreamStatus) {
            case 403 -> envelope(HttpStatus.FORBIDDEN, "FORBIDDEN_MEMBER", null);
            case 429 -> envelope(HttpStatus.TOO_MANY_REQUESTS, "CONCURRENCY_LIMIT", true);
            default -> envelope(HttpStatus.BAD_GATEWAY, "UPSTREAM_ERROR", null);
        };
    }

    private ResponseEntity<byte[]> envelope(HttpStatus status, String code, Boolean retryable) {
        try {
            Map<String, Object> err = retryable == null
                    ? Map.of("code", code)
                    : Map.of("code", code, "retryable", retryable);
            return json(status, objectMapper.writeValueAsBytes(Map.of("error", err)));
        } catch (Exception e) {
            return ResponseEntity.status(status).build();
        }
    }

    private static ResponseEntity<byte[]> json(HttpStatus status, byte[] body) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }
}
