package br.com.archbase.security.mfa;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Locale;

/**
 * TOTP (Time-based One-Time Password, RFC 6238 sobre HOTP/RFC 4226) para MFA/2FA.
 * Implementação pura em JDK (HmacSHA1 + Base32 RFC 4648), compatível com Google
 * Authenticator/Authy: segredo em Base32, período de 30s, 6 dígitos.
 *
 * <p>Sem estado e sem dependências novas — apenas geração/validação de códigos e a
 * URI de provisionamento ({@code otpauth://}) para o QR Code de enrolamento.
 */
@Service
public class TotpService {

    private static final String ALGORITHM = "HmacSHA1";
    private static final int DEFAULT_DIGITS = 6;
    private static final int DEFAULT_STEP_SECONDS = 30;
    /** Janela de tolerância (passos para trás/frente) para compensar clock skew. */
    private static final int DEFAULT_WINDOW = 1;
    private static final int SECRET_BYTES = 20; // 160 bits (recomendado pela RFC 4226)

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private final SecureRandom random = new SecureRandom();

    /** Gera um novo segredo aleatório de 160 bits, codificado em Base32 (sem padding). */
    public String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        random.nextBytes(bytes);
        return base32Encode(bytes);
    }

    /**
     * URI de provisionamento {@code otpauth://totp/...} para o QR Code no app autenticador.
     *
     * @param secret      segredo Base32 do usuário
     * @param accountName identificação da conta (e-mail/login)
     * @param issuer      emissor (nome do sistema/tenant)
     */
    public String provisioningUri(String secret, String accountName, String issuer) {
        String label = urlEncode(issuer) + ":" + urlEncode(accountName);
        return "otpauth://totp/" + label
                + "?secret=" + secret
                + "&issuer=" + urlEncode(issuer)
                + "&algorithm=SHA1"
                + "&digits=" + DEFAULT_DIGITS
                + "&period=" + DEFAULT_STEP_SECONDS;
    }

    /** Valida o código para o instante atual, tolerando ±1 passo (clock skew). */
    public boolean verifyCode(String secret, String code) {
        return verifyCode(secret, code, System.currentTimeMillis(), DEFAULT_WINDOW);
    }

    /** Valida o código para um instante dado, aceitando qualquer passo dentro da janela. */
    public boolean verifyCode(String secret, String code, long timeMillis, int window) {
        if (secret == null || code == null) {
            return false;
        }
        String normalizado = code.trim();
        long counter = timeMillis / 1000L / DEFAULT_STEP_SECONDS;
        for (int erro = -window; erro <= window; erro++) {
            String esperado = generateCodeForCounter(secret, counter + erro, DEFAULT_DIGITS);
            if (constantTimeEquals(esperado, normalizado)) {
                return true;
            }
        }
        return false;
    }

    /** Código TOTP para um instante (usado em testes e verificação). */
    public String generateCode(String secret, long timeMillis) {
        return generateCode(secret, timeMillis, DEFAULT_DIGITS, DEFAULT_STEP_SECONDS);
    }

    String generateCode(String secret, long timeMillis, int digits, int stepSeconds) {
        long counter = timeMillis / 1000L / stepSeconds;
        return generateCodeForCounter(secret, counter, digits);
    }

    // ===== HOTP (RFC 4226) =====

    private String generateCodeForCounter(String base32Secret, long counter, int digits) {
        byte[] key = base32Decode(base32Secret);
        byte[] data = new byte[8];
        for (int i = 7; i >= 0; i--) {
            data[i] = (byte) (counter & 0xff);
            counter >>>= 8;
        }
        byte[] hash = hmacSha1(key, data);
        int offset = hash[hash.length - 1] & 0x0f;
        int binary = ((hash[offset] & 0x7f) << 24)
                | ((hash[offset + 1] & 0xff) << 16)
                | ((hash[offset + 2] & 0xff) << 8)
                | (hash[offset + 3] & 0xff);
        int otp = binary % (int) Math.pow(10, digits);
        return String.format(Locale.ROOT, "%0" + digits + "d", otp);
    }

    private byte[] hmacSha1(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key, ALGORITHM));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao calcular HMAC-SHA1 para TOTP", e);
        }
    }

    // ===== Base32 (RFC 4648, sem padding) =====

    static String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xff);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1f;
                bitsLeft -= 5;
                sb.append(BASE32_ALPHABET.charAt(index));
            }
        }
        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1f;
            sb.append(BASE32_ALPHABET.charAt(index));
        }
        return sb.toString();
    }

    static byte[] base32Decode(String base32) {
        String limpo = base32.trim().replace("=", "").toUpperCase(Locale.ROOT);
        int buffer = 0;
        int bitsLeft = 0;
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        for (char c : limpo.toCharArray()) {
            int val = BASE32_ALPHABET.indexOf(c);
            if (val < 0) {
                continue; // ignora separadores/whitespace
            }
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out.write((buffer >> (bitsLeft - 8)) & 0xff);
                bitsLeft -= 8;
            }
        }
        return out.toByteArray();
    }

    private String urlEncode(String value) {
        return java.net.URLEncoder.encode(value == null ? "" : value, java.nio.charset.StandardCharsets.UTF_8);
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
