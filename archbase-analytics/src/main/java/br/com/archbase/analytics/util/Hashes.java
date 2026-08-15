package br.com.archbase.analytics.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** SHA-256 utilitário (hash de contexto de auditoria, hash de registro de sync). */
public final class Hashes {

    private static final String SEPARATOR = "|";

    private Hashes() {
    }

    public static String sha256(Object... fields) {
        StringBuilder sb = new StringBuilder();
        for (Object f : fields) {
            sb.append(f != null ? f : "").append(SEPARATOR);
        }
        return HexFormat.of().formatHex(
                digest().digest(sb.toString().getBytes(StandardCharsets.UTF_8)));
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM sem SHA-256", e);
        }
    }
}
