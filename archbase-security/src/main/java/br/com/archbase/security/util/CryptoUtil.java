package br.com.archbase.security.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Primitivas AES-GCM usadas pela cifragem de segredos em repouso.
 *
 * <p><b>Charset explícito.</b> A conversão texto↔bytes usa UTF-8 em vez do charset padrão da
 * plataforma. Com o padrão, o valor cifrado numa JVM e decifrado em outra com {@code file.encoding}
 * diferente devolvia texto corrompido em qualquer caractere acentuado — e o projeto compila para
 * Java 17, onde o padrão ainda depende do locale (o UTF-8 universal só veio no 18, via JEP 400).
 *
 * <p>Se algum valor acentuado foi cifrado em ambiente cujo padrão não era UTF-8, ele precisa ser
 * recifrado; use {@code ArchbaseColumnReencryptor}.
 */
public class CryptoUtil {
    private static final String ALGORITHM = "AES";
    private static final int GCM_IV_LENGTH = 12; // 12 bytes for GCM IV
    private static final int GCM_TAG_LENGTH = 16; // 16 bytes for GCM authentication tag

    // Geração da chave AES-GCM
    public static String generateSecretKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
        keyGen.init(256);
        SecretKey key = keyGen.generateKey();
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    // Criptografia AES-GCM
    public static String encrypt(String data, String base64Key, byte[] iv) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(Base64.getDecoder().decode(base64Key), ALGORITHM);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);
        byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedData);
    }

    // Decriptografia AES-GCM
    public static String decrypt(String encryptedData, String base64Key, byte[] iv) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(Base64.getDecoder().decode(base64Key), ALGORITHM);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
        byte[] decryptedData = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decryptedData, StandardCharsets.UTF_8);
    }

    public static String encryptWithIv(String data, String base64Key) throws Exception {
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);

        SecretKeySpec keySpec = new SecretKeySpec(Base64.getDecoder().decode(base64Key), ALGORITHM);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);

        byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        byte[] encryptedDataWithIv = new byte[iv.length + encryptedData.length];
        System.arraycopy(iv, 0, encryptedDataWithIv, 0, iv.length);
        System.arraycopy(encryptedData, 0, encryptedDataWithIv, iv.length, encryptedData.length);

        return Base64.getEncoder().encodeToString(encryptedDataWithIv);
    }

    // Decriptografia com IV extraído
    public static String decryptWithIv(String encryptedDataWithIv, String base64Key) throws Exception {
        byte[] encryptedDataWithIvBytes = Base64.getDecoder().decode(encryptedDataWithIv);
        byte[] iv = Arrays.copyOfRange(encryptedDataWithIvBytes, 0, 12);
        byte[] encryptedData = Arrays.copyOfRange(encryptedDataWithIvBytes, 12, encryptedDataWithIvBytes.length);

        SecretKeySpec keySpec = new SecretKeySpec(Base64.getDecoder().decode(base64Key), ALGORITHM);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);

        byte[] decryptedData = cipher.doFinal(encryptedData);
        return new String(decryptedData, StandardCharsets.UTF_8);
    }
}

