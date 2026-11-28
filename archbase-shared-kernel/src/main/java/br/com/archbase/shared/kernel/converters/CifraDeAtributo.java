package br.com.archbase.shared.kernel.converters;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Formato atual da cifra de atributo: AES-GCM, IV aleatório de 12 bytes por valor, tag de 128 bits.
 *
 * <p>O valor gravado é {@code v2:} seguido do Base64 de {@code IV || texto cifrado}. O prefixo não colide com o
 * formato anterior, que é Base64 puro (o alfabeto do Base64 não tem {@code :}); é assim que a leitura distingue os
 * dois e o dado já gravado continua legível.
 */
public final class CifraDeAtributo {

    /** Prefixo do formato atual. */
    public static final String PREFIXO = "v2:";

    private static final String TRANSFORMACAO = "AES/GCM/NoPadding";
    private static final int TAMANHO_IV = 12;
    private static final int TAMANHO_TAG_BITS = 128;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private CifraDeAtributo() {
    }

    /** {@code true} se o valor está no formato atual. */
    public static boolean formatoAtual(String valor) {
        return valor != null && valor.startsWith(PREFIXO);
    }

    public static String cifrar(byte[] chave, String texto) {
        try {
            byte[] iv = new byte[TAMANHO_IV];
            ALEATORIO.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMACAO);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(chave, "AES"), new GCMParameterSpec(TAMANHO_TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            byte[] saida = ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array();
            return PREFIXO + Base64.getEncoder().encodeToString(saida);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao cifrar atributo", e);
        }
    }

    public static String decifrar(byte[] chave, String valor) {
        try {
            byte[] entrada = Base64.getDecoder().decode(valor.substring(PREFIXO.length()));
            if (entrada.length <= TAMANHO_IV) {
                throw new IllegalStateException("Valor cifrado truncado");
            }
            Cipher cipher = Cipher.getInstance(TRANSFORMACAO);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(chave, "AES"),
                    new GCMParameterSpec(TAMANHO_TAG_BITS, entrada, 0, TAMANHO_IV));
            byte[] claro = cipher.doFinal(entrada, TAMANHO_IV, entrada.length - TAMANHO_IV);
            return new String(claro, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Falha ao decifrar atributo: chave errada ou valor adulterado", e);
        }
    }

    /**
     * Chave AES a partir do Base64 de 16, 24 ou 32 bytes ({@code openssl rand -base64 32}).
     *
     * @throws IllegalStateException se não for Base64 válido ou tiver outro tamanho
     */
    public static byte[] chaveBase64(String base64, String origem) {
        byte[] chave;
        try {
            chave = Base64.getDecoder().decode(base64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(origem + " não é Base64 válido", e);
        }
        if (chave.length != 16 && chave.length != 24 && chave.length != 32) {
            throw new IllegalStateException(origem + " deve ter 16, 24 ou 32 bytes depois de decodificada; tem "
                    + chave.length);
        }
        return chave;
    }
}
