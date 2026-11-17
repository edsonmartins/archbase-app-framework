package br.com.archbase.controlplane.token;

import io.jsonwebtoken.security.Jwk;
import io.jsonwebtoken.security.JwkSet;
import io.jsonwebtoken.security.Jwks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.Key;
import java.security.PublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Chaves públicas do Control Plane, por {@code kid}.
 * <ul>
 * <li>Na subida, lê a cópia local (funciona offline) e tenta buscar o JWKS remoto.</li>
 * <li>{@code kid} desconhecido dispara uma busca — no máximo uma a cada {@code refreshMinInterval}, para um token
 * forjado com {@code kid} aleatório não virar enxurrada de requisições ao Control Plane. A chave nova só é aceita
 * depois que essa busca a traz.</li>
 * <li>Cada busca bem-sucedida substitui o conjunto inteiro (chave retirada do JWKS deixa de valer) e regrava a
 * cópia local.</li>
 * <li>Busca que falha mantém as chaves que já estavam carregadas.</li>
 * </ul>
 * Só chaves públicas Ed25519 são carregadas; qualquer outra entrada do JWKS é ignorada.
 *
 * @since 3.4.6
 */
public class JwksKeyStore {

    private static final Logger log = LoggerFactory.getLogger(JwksKeyStore.class);

    private final String url;
    private final Path localPath;
    private final Duration refreshMinInterval;
    private final Duration timeout;
    private final Clock clock;
    private final HttpClient httpClient;

    private volatile Map<String, PublicKey> keys = Map.of();
    private volatile Instant lastFetchAttempt = Instant.EPOCH;

    public JwksKeyStore(String url, String localPath, Duration refreshMinInterval, Duration timeout, Clock clock) {
        this.url = blankToNull(url);
        this.localPath = blankToNull(localPath) == null ? null : Path.of(localPath);
        this.refreshMinInterval = refreshMinInterval;
        this.timeout = timeout;
        this.clock = clock;
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    /** Carrega a cópia local e tenta a busca remota. Nunca falha: sem chave nenhuma, todo token é recusado. */
    public void initialize() {
        loadLocalCopy();
        if (url != null) {
            refresh();
        }
        if (keys.isEmpty()) {
            log.error("controlplane.jwks.empty url={} localPath={} — nenhum token do Control Plane será aceito "
                    + "até o JWKS ser alcançado", url, localPath);
        }
    }

    /** A chave do {@code kid}; busca o JWKS se ela for desconhecida e o intervalo mínimo tiver passado. */
    public Optional<PublicKey> resolve(String kid) {
        if (kid == null || kid.isBlank()) {
            return Optional.empty();
        }
        PublicKey key = keys.get(kid);
        if (key != null) {
            return Optional.of(key);
        }
        if (url != null && refreshAllowed()) {
            log.info("controlplane.jwks.unknown_kid kid={} — buscando o JWKS", kid);
            refresh();
            return Optional.ofNullable(keys.get(kid));
        }
        return Optional.empty();
    }

    /** {@code kid}s carregados no momento (diagnóstico e testes). */
    public java.util.Set<String> knownKeyIds() {
        return keys.keySet();
    }

    private synchronized boolean refreshAllowed() {
        Instant now = clock.instant();
        if (Duration.between(lastFetchAttempt, now).compareTo(refreshMinInterval) < 0) {
            return false;
        }
        lastFetchAttempt = now;
        return true;
    }

    /** Busca o JWKS remoto. Devolve se a busca deu certo. */
    synchronized boolean refresh() {
        lastFetchAttempt = clock.instant();
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(timeout)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("controlplane.jwks.fetch_failed url={} status={} — mantendo {} chave(s) carregada(s)",
                        url, response.statusCode(), keys.size());
                return false;
            }
            Map<String, PublicKey> parsed = parse(response.body());
            if (parsed.isEmpty()) {
                log.warn("controlplane.jwks.fetch_empty url={} — JWKS sem chave Ed25519; mantendo as atuais", url);
                return false;
            }
            keys = Map.copyOf(parsed);
            log.info("controlplane.jwks.loaded source=remote kids={}", keys.keySet());
            writeLocalCopy(response.body());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("controlplane.jwks.fetch_failed url={} interrompida", url);
            return false;
        } catch (IOException | RuntimeException e) {
            log.warn("controlplane.jwks.fetch_failed url={} erro={} — mantendo {} chave(s) carregada(s)",
                    url, e.getMessage(), keys.size());
            return false;
        }
    }

    private void loadLocalCopy() {
        if (localPath == null || !Files.isRegularFile(localPath)) {
            return;
        }
        try {
            Map<String, PublicKey> parsed = parse(Files.readString(localPath, StandardCharsets.UTF_8));
            keys = Map.copyOf(parsed);
            log.info("controlplane.jwks.loaded source=local path={} kids={}", localPath, keys.keySet());
        } catch (IOException | RuntimeException e) {
            log.warn("controlplane.jwks.local_unreadable path={} erro={}", localPath, e.getMessage());
        }
    }

    private void writeLocalCopy(String json) {
        if (localPath == null) {
            return;
        }
        try {
            Path parent = localPath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path tmp = Files.createTempFile(parent, ".jwks", ".tmp");
            Files.writeString(tmp, json, StandardCharsets.UTF_8);
            Files.move(tmp, localPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException | RuntimeException e) {
            log.warn("controlplane.jwks.local_write_failed path={} erro={}", localPath, e.getMessage());
        }
    }

    static Map<String, PublicKey> parse(String json) {
        JwkSet set = Jwks.setParser().build().parse(json);
        Map<String, PublicKey> parsed = new HashMap<>();
        for (Jwk<?> jwk : set.getKeys()) {
            Key key = jwk.toKey();
            if (jwk.getId() != null && key instanceof PublicKey publicKey && isEd25519(publicKey)) {
                parsed.put(jwk.getId(), publicKey);
            }
        }
        return parsed;
    }

    private static boolean isEd25519(PublicKey key) {
        String algorithm = key.getAlgorithm();
        return "Ed25519".equalsIgnoreCase(algorithm) || "EdDSA".equalsIgnoreCase(algorithm);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
