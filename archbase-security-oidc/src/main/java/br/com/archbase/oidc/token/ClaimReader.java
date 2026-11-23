package br.com.archbase.oidc.token;

import java.util.Map;

/**
 * Lê um claim por <b>nome</b> ou por <b>caminho</b>.
 * <p>
 * O nome exato tem precedência: claims com namespace do Auth0 são URLs com pontos
 * ({@code https://empresa.com/roles}) e quebrariam se fossem sempre tratados como caminho. Só quando não há claim com
 * aquele nome o valor é procurado como caminho com pontos ({@code realm_access.roles}, Keycloak).
 *
 * @since 3.5.0
 */
public final class ClaimReader {

    private ClaimReader() {
    }

    public static Object read(Map<String, Object> claims, String nameOrPath) {
        if (claims == null || nameOrPath == null || nameOrPath.isBlank()) {
            return null;
        }
        if (claims.containsKey(nameOrPath)) {
            return claims.get(nameOrPath);
        }
        Object current = claims;
        for (String part : nameOrPath.split("\\.")) {
            if (!(current instanceof Map<?, ?> map) || !map.containsKey(part)) {
                return null;
            }
            current = map.get(part);
        }
        return current;
    }

    /** Texto do claim; números viram texto; qualquer outro tipo (lista, objeto) é {@code null}. */
    public static String readString(Map<String, Object> claims, String nameOrPath) {
        Object value = read(claims, nameOrPath);
        if (value instanceof String s) {
            return s.isBlank() ? null : s;
        }
        if (value instanceof Number n) {
            return n.toString();
        }
        return null;
    }

    /** {@code true} só para o booleano {@code true} ou o texto {@code "true"}; ausente ou outro valor é falso. */
    public static boolean readTrue(Map<String, Object> claims, String nameOrPath) {
        Object value = read(claims, nameOrPath);
        return Boolean.TRUE.equals(value) || (value instanceof String s && "true".equalsIgnoreCase(s.trim()));
    }
}
