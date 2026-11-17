package br.com.archbase.controlplane.it;

import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Control Plane de teste: emite tokens Ed25519 e serve o JWKS por HTTP numa porta livre. As chaves publicadas
 * podem mudar durante o teste (rotação) e o servidor pode ser desligado (Control Plane fora do ar).
 */
public final class FakeControlPlane implements AutoCloseable {

    public static final String ISSUER = "https://cp.teste";
    public static final String AUDIENCE = "https://powerview.cliente.teste";

    private final Map<String, KeyPair> keys = new ConcurrentHashMap<>();
    private final Map<String, Boolean> published = new ConcurrentHashMap<>();
    private final AtomicInteger jwksRequests = new AtomicInteger();
    private volatile boolean up = true;
    private final HttpServer server;

    public FakeControlPlane() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.createContext("/.well-known/jwks.json", exchange -> {
            jwksRequests.incrementAndGet();
            byte[] body = (up ? jwksJson() : "").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(up ? 200 : 503, body.length == 0 ? -1 : body.length);
            if (body.length > 0) {
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            }
            exchange.close();
        });
        server.start();
    }

    public String jwksUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/.well-known/jwks.json";
    }

    /** Cria uma chave; publicada no JWKS se {@code publish}. */
    public FakeControlPlane addKey(String kid, boolean publish) {
        keys.put(kid, newKeyPair());
        published.put(kid, publish);
        return this;
    }

    public void publish(String kid) {
        published.put(kid, true);
    }

    public void setUp(boolean up) {
        this.up = up;
    }

    public int jwksRequests() {
        return jwksRequests.get();
    }

    public String jwksJson() {
        String entries = keys.entrySet().stream()
                .filter(e -> Boolean.TRUE.equals(published.get(e.getKey())))
                .map(e -> "{\"kty\":\"OKP\",\"crv\":\"Ed25519\",\"use\":\"sig\",\"kid\":\"" + e.getKey()
                        + "\",\"x\":\"" + rawX(e.getValue().getPublic()) + "\"}")
                .collect(Collectors.joining(","));
        return "{\"keys\":[" + entries + "]}";
    }

    /** Token de sessão válido para o tenant, assinado com {@code kid}. */
    public Token session(String kid, String sub, String email, String tenant, String adm) {
        return new Token(kid).claim("sub", sub).claim("email", email).claim("tnt", tenant).claim("adm", adm);
    }

    /** Licença da instância (contrato assumido: token_use=license, sub=instância, prd=produto). */
    public Token license(String kid, String instanceId, String product, Instant expiresAt) {
        return new Token(kid).claim("sub", instanceId).claim("prd", product).claim("token_use", "license")
                .expiresAt(expiresAt);
    }

    /** Token de instância (serviço a serviço). */
    public Token instance(String kid, String instanceId) {
        return new Token(kid).claim("sub", instanceId).claim("token_use", "instance");
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private static KeyPair newKeyPair() {
        try {
            return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Os 32 bytes da chave pública, em base64url: o encoding X.509 do Ed25519 tem 12 bytes de prefixo. */
    private static String rawX(PublicKey key) {
        byte[] encoded = key.getEncoded();
        byte[] raw = Arrays.copyOfRange(encoded, encoded.length - 32, encoded.length);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }

    /** Construtor fluente de token, com tudo válido por padrão. */
    public final class Token {

        private final String kid;
        private final Map<String, Object> claims = new LinkedHashMap<>();
        private String issuer = ISSUER;
        private String audience = AUDIENCE;
        private Instant expiration = Instant.now().plus(Duration.ofMinutes(10));
        private PrivateKey signingKey;

        private Token(String kid) {
            this.kid = kid;
        }

        public Token claim(String name, Object value) {
            if (value == null) {
                claims.remove(name);
            } else {
                claims.put(name, value);
            }
            return this;
        }

        public Token without(String name) {
            claims.remove(name);
            return this;
        }

        public Token issuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        public Token audience(String audience) {
            this.audience = audience;
            return this;
        }

        public Token expiresAt(Instant expiration) {
            this.expiration = expiration;
            return this;
        }

        /** Assina com outra chave, mantendo o {@code kid} — simula assinatura forjada. */
        public Token signedByStranger() {
            this.signingKey = newKeyPair().getPrivate();
            return this;
        }

        public String compact() {
            PrivateKey key = signingKey != null ? signingKey : keys.get(kid).getPrivate();
            JwtBuilder builder = Jwts.builder()
                    .header().keyId(kid).and()
                    .issuer(issuer)
                    .audience().add(audience).and()
                    .id(UUID.randomUUID().toString())
                    .issuedAt(Date.from(Instant.now().minusSeconds(5)))
                    .expiration(expiration == null ? null : Date.from(expiration));
            claims.forEach((name, value) -> {
                if ("sub".equals(name)) {
                    builder.subject((String) value);
                } else {
                    builder.claim(name, value);
                }
            });
            return builder.signWith(key, Jwts.SIG.EdDSA).compact();
        }

        public String bearer() {
            return "Bearer " + compact();
        }
    }
}
