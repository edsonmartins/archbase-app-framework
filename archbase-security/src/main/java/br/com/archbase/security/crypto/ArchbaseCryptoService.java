package br.com.archbase.security.crypto;

import java.util.Base64;

import br.com.archbase.security.util.CryptoUtil;

/**
 * Serviço reutilizável de cifragem de segredos em repouso (AES-GCM com IV aleatório por valor,
 * autenticado). Reaproveita {@link CryptoUtil#encryptWithIv}/{@link CryptoUtil#decryptWithIv}
 * (o IV vai embutido no ciphertext) e centraliza a chave, para os projetos apenas injetarem
 * este bean em vez de espalhar primitivas de cripto.
 *
 * <p>A chave vem de {@code archbase.security.crypto.key} — deve ser provida por variável de
 * ambiente / secret manager, nunca versionada. {@link CryptoUtil} espera a chave em Base64; se a
 * configurada não for Base64 válido de 16/24/32 bytes, usamos o Base64 dos seus bytes crus
 * (aceita também uma string crua de 32 chars = 256 bits).
 */
public class ArchbaseCryptoService {

    private final String base64Key;

    public ArchbaseCryptoService(String configuredKey) {
        this.base64Key = normalizeToBase64Key(configuredKey);
    }

    /** Cifra um valor com AES-GCM (IV aleatório embutido). {@code null} entra, {@code null} sai. */
    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            return CryptoUtil.encryptWithIv(plainText, base64Key);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao cifrar valor (AES-GCM)", e);
        }
    }

    /** Decifra um valor cifrado com {@link #encrypt(String)} (AES-GCM). */
    public String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }
        try {
            return CryptoUtil.decryptWithIv(cipherText, base64Key);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao decifrar valor (AES-GCM)", e);
        }
    }

    private static String normalizeToBase64Key(String configuredKey) {
        if (configuredKey == null || configuredKey.isEmpty()) {
            return "";
        }
        try {
            int len = Base64.getDecoder().decode(configuredKey).length;
            if (len == 16 || len == 24 || len == 32) {
                return configuredKey; // já é Base64 de uma chave AES válida
            }
        } catch (IllegalArgumentException ignored) {
            // não é Base64; trata como chave crua abaixo
        }
        return Base64.getEncoder().encodeToString(configuredKey.getBytes());
    }
}
