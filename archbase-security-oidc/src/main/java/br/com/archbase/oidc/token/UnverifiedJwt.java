package br.com.archbase.oidc.token;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * Lê o {@code iss} de um JWS <b>sem verificar a assinatura</b> — só para decidir qual provedor deve validá-lo.
 * <p>
 * Roda em toda requisição com {@code Authorization: Bearer}, antes de autenticar, com entrada controlada pelo
 * cliente. Por isso confere a forma antes de decodificar: tamanho limitado, só o alfabeto base64url, sem padding
 * (CVE-2026-114938: entrada malformada no decodificador intrínseco da JVM corrompe o heap). Qualquer desvio devolve
 * vazio — o token simplesmente não é deste módulo.
 *
 * @since 3.5.0
 */
public final class UnverifiedJwt {

    /** Tokens reais de IdP ficam bem abaixo; acima disso não vale nem o parse. */
    static final int MAX_TOKEN_LENGTH = 16 * 1024;

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private UnverifiedJwt() {
    }

    public static Optional<String> issuer(String token) {
        if (token == null || token.length() > MAX_TOKEN_LENGTH) {
            return Optional.empty();
        }
        int first = token.indexOf('.');
        int second = first < 0 ? -1 : token.indexOf('.', first + 1);
        if (first <= 0 || second < 0 || token.indexOf('.', second + 1) >= 0) {
            return Optional.empty(); // JWS compacto tem exatamente três partes (JWE tem cinco)
        }
        String payload = token.substring(first + 1, second);
        if (!isCanonicalBase64Url(payload)) {
            return Optional.empty();
        }
        try {
            JsonNode node = JSON.readTree(new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8));
            JsonNode iss = node == null ? null : node.get("iss");
            return iss != null && iss.isString() && !iss.asString().isBlank()
                    ? Optional.of(iss.asString()) : Optional.empty();
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    static boolean isCanonicalBase64Url(String segment) {
        int len = segment.length();
        if (len == 0 || len % 4 == 1) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            char c = segment.charAt(i);
            if (!((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '_')) {
                return false;
            }
        }
        return true;
    }
}
