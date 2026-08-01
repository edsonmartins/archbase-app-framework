package br.com.archbase.security.util;

/**
 * Máscara de credenciais para log.
 *
 * <p>Token de API é a credencial em si — não um identificador dela. Registrá-lo inteiro entrega
 * acesso a quem lê o log: agregador, arquivo em disco, anexo de ticket de suporte. O prefixo é
 * suficiente para correlacionar linhas de uma mesma requisição e curto demais para reutilizar.
 */
public final class TokenMaskUtil {

    private static final int VISIBLE_PREFIX = 8;

    private TokenMaskUtil() {
        // Classe utilitária
    }

    public static String mask(String token) {
        if (token == null || token.length() < VISIBLE_PREFIX) {
            return "***";
        }
        return token.substring(0, VISIBLE_PREFIX) + "…";
    }
}
