package br.com.archbase.security.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Hash de token de API para guarda em repouso.
 *
 * <p><b>Por que hash e não cifra.</b> O token só precisa ser <i>comparado</i>, nunca recuperado.
 * Guardá-lo cifrado exigiria uma chave que, se vazar junto com o dump, devolve todos os tokens em
 * claro; guardá-lo em hash torna o dump inútil para autenticar.
 *
 * <p><b>Por que SHA-256 puro e não bcrypt/argon2.</b> Os algoritmos de senha são lentos de
 * propósito para resistir a força bruta sobre segredos de baixa entropia — e são incompatíveis com
 * busca indexada, porque cada linha tem sal próprio (seria varredura da tabela inteira a cada
 * requisição autenticada). O token aqui é um UUID v4: 122 bits de aleatoriedade, fora do alcance de
 * força bruta ou rainbow table. Nesse regime, o hash rápido e determinístico é a escolha correta —
 * é o mesmo desenho dos personal access tokens do GitHub.
 *
 * <p>Sem sal, de propósito: o lookup precisa ser {@code WHERE token_hash = ?}.
 */
public final class ApiTokenHasher {

    /** Tamanho do hash em hexadecimal — usado também como largura da coluna. */
    public static final int HASH_LENGTH = 64;

    private ApiTokenHasher() {
        // Classe utilitária
    }

    /**
     * @return SHA-256 do token em hexadecimal minúsculo, ou {@code null} se o token for {@code null}
     */
    public static String hash(String token) {
        if (token == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 é obrigatório em toda JVM; se faltar, o ambiente está quebrado.
            throw new IllegalStateException("SHA-256 indisponível nesta JVM", e);
        }
    }
}
