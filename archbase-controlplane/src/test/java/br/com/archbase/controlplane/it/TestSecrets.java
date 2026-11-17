package br.com.archbase.controlplane.it;

import java.security.SecureRandom;
import java.util.Base64;

/** Segredos de teste gerados a cada execução: nenhum valor fica gravado no código. */
final class TestSecrets {

    private static final String JWT_KEY = randomBase64(32);

    private TestSecrets() {
    }

    /** Chave HS256 do JWT próprio do Archbase (archbase.security.jwt.secret-key). */
    static String legacyJwtKey() {
        return JWT_KEY;
    }

    private static String randomBase64(int bytes) {
        byte[] key = new byte[bytes];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
