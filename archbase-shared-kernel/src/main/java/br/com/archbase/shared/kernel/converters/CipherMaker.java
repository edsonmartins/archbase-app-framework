package br.com.archbase.shared.kernel.converters;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import javax.crypto.Cipher;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;

/**
 * Classe utilitário para gerar uma instância de {@link Cipher}
 *
 * @author edsonmartins
 */
public class CipherMaker {

    private static final String CIPHER_INSTANCE_NAME = "AES/CBC/PKCS5Padding";
    private static final String SECRET_KEY_ALGORITHM = "AES";

    /**
     * @param encryptionMode - decide se deve criptografar ou descriptografar os dados. Valores aceitos:
     *                       {@link Cipher#ENCRYPT_MODE} para criptografia e
     *                       {@link Cipher#DECRYPT_MODE} para descriptografia.
     * @param key            - a chave a ser usada para criptografar ou descriptografar dados. Isso pode
     *                       ser uma string simples como "MySecretKey" ou um mais complexo, difícil
     *                       adivinhar string mais longa
     * @return
     * @throws InvalidKeyException
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidAlgorithmParameterException
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     */
    public Cipher configureAndGetInstance(int encryptionMode, String key)
            throws InvalidKeyException, NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, InvalidAlgorithmParameterException {

        Cipher cipher = Cipher.getInstance(CIPHER_INSTANCE_NAME);
        Key secretKey = new SecretKeySpec(key.getBytes(), SECRET_KEY_ALGORITHM);

        byte[] ivBytes = new byte[cipher.getBlockSize()];
        AlgorithmParameterSpec algorithmParameters = new IvParameterSpec(
                ivBytes);

        cipher.init(encryptionMode, secretKey, algorithmParameters);
        return cipher;
    }
}
