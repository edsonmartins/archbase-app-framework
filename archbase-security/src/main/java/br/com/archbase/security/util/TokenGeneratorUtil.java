package br.com.archbase.security.util;

import java.security.SecureRandom;

public class TokenGeneratorUtil {
    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int TOKEN_LENGTH = 13;
    private static final SecureRandom random = new SecureRandom();

    public static String generateAlphaNumericToken() {
        StringBuilder token = new StringBuilder(TOKEN_LENGTH);
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            token.append(CHARACTERS.charAt(index));
        }
        return token.toString();
    }

    // Novo método para gerar número com 8 dígitos
    public static String generateNumericToken() {
        StringBuilder token = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            int digit = random.nextInt(10); // Gera um dígito entre 0 e 9
            token.append(digit);
        }
        return token.toString();
    }
}