package br.com.archbase.oidc.it;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IdP OIDC de teste: serve JWKS e discovery por HTTP numa porta livre e emite tokens assinados. Vários emissores
 * convivem no mesmo servidor (um por "IdP" simulado), com o mesmo conjunto de chaves publicado.
 */
public final class FakeOidcProvider implements AutoCloseable {

    private final HttpServer server;
    private final Map<String, JWK> keys = new ConcurrentHashMap<>();
    private final Map<String, Boolean> published = new ConcurrentHashMap<>();

    public FakeOidcProvider() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String body;
            int status = 200;
            if (path.endsWith("/jwks")) {
                body = jwks();
            } else if (path.endsWith("/.well-known/openid-configuration")) {
                String issuer = baseUrl() + path.substring(0, path.length() - "/.well-known/openid-configuration".length());
                body = "{\"issuer\":\"" + issuer + "\",\"jwks_uri\":\"" + issuer + "/jwks\","
                        + "\"id_token_signing_alg_values_supported\":[\"RS256\"],\"subject_types_supported\":[\"public\"],"
                        + "\"response_types_supported\":[\"code\"]}";
            } else {
                status = 404;
                body = "";
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            }
            exchange.close();
        });
        server.start();
        addRsaKey("rsa-1", true);
        addEcKey("ec-1", true);
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    /** Emissor simulado em {@code <base>/<name>}; JWKS em {@code <issuer>/jwks}. */
    public String issuer(String name) {
        return baseUrl() + "/" + name;
    }

    public String jwksUri(String name) {
        return issuer(name) + "/jwks";
    }

    public FakeOidcProvider addRsaKey(String kid, boolean publish) {
        try {
            keys.put(kid, new RSAKeyGenerator(2048).keyID(kid).generate());
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
        published.put(kid, publish);
        return this;
    }

    public FakeOidcProvider addEcKey(String kid, boolean publish) {
        try {
            keys.put(kid, new ECKeyGenerator(Curve.P_256).keyID(kid).generate());
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
        published.put(kid, publish);
        return this;
    }

    public void publish(String kid, boolean publish) {
        published.put(kid, publish);
    }

    private String jwks() {
        List<JWK> pub = new ArrayList<>();
        keys.forEach((kid, k) -> {
            if (Boolean.TRUE.equals(published.get(kid))) {
                pub.add(k.toPublicJWK());
            }
        });
        return new JWKSet(pub).toString();
    }

    public Token token(String issuer) {
        return new Token(issuer);
    }

    /** Construtor de token com padrões válidos; cada teste muda só o que interessa. */
    public final class Token {
        private final Map<String, Object> claims = new LinkedHashMap<>();
        private String kid = "rsa-1";
        private Instant exp = Instant.now().plusSeconds(300);
        private boolean withExp = true;

        private Token(String issuer) {
            claims.put("iss", issuer);
            claims.put("sub", "sub-padrao");
            claims.put("iat", Date.from(Instant.now()));
        }

        public Token claim(String name, Object value) {
            if (value == null) {
                claims.remove(name);
            } else {
                claims.put(name, value);
            }
            return this;
        }

        public Token aud(String... aud) {
            return claim("aud", List.of(aud));
        }

        public Token kid(String kid) {
            this.kid = kid;
            return this;
        }

        /** Também recua o {@code iat}: token real expirado foi emitido antes de expirar. */
        public Token expiresAt(Instant exp) {
            this.exp = exp;
            claims.put("iat", Date.from(exp.minusSeconds(300)));
            return this;
        }

        public Token withoutExp() {
            this.withExp = false;
            return this;
        }

        public String sign() {
            JWK key = keys.get(kid);
            try {
                JWTClaimsSet.Builder b = new JWTClaimsSet.Builder();
                claims.forEach(b::claim);
                if (withExp) {
                    b.expirationTime(Date.from(exp));
                }
                JWSAlgorithm alg;
                JWSSigner signer;
                if (key instanceof RSAKey rsa) {
                    alg = JWSAlgorithm.RS256;
                    signer = new RSASSASigner(rsa);
                } else {
                    alg = JWSAlgorithm.ES256;
                    signer = new ECDSASigner((ECKey) key);
                }
                SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(alg).keyID(kid).build(), b.build());
                jwt.sign(signer);
                return jwt.serialize();
            } catch (JOSEException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
