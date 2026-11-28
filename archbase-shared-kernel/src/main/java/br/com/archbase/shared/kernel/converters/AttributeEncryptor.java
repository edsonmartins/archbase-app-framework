package br.com.archbase.shared.kernel.converters;

import jakarta.persistence.AttributeConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Cifra um atributo {@code String} com a chave de {@code aes.encryption.key} (o texto da propriedade, com 16, 24 ou
 * 32 caracteres, como sempre foi).
 *
 * <p>Grava no formato atual ({@link CifraDeAtributo}: AES-GCM, IV aleatório, prefixo {@code v2:}) e lê os dois: o
 * atual e o anterior, AES/ECB sem prefixo. Cada linha passa ao formato atual na próxima vez que for gravada.
 *
 * <p>Até a 3.5.0 a cifra era AES/ECB — o mesmo texto dava sempre o mesmo valor, e blocos iguais se repetiam — com um
 * único {@code Cipher} compartilhado entre threads, que não é seguro para uso concorrente. E a propriedade era
 * exigida na subida, bastando a varredura do pacote; agora é exigida só no primeiro uso.
 */
@Component
public class AttributeEncryptor implements AttributeConverter<String, String> {

    private static final String AES_ECB = "AES";

    @Value("${aes.encryption.key:}")
    private String encryptionKey;

    private volatile byte[] key;

    public AttributeEncryptor() {
    }

    /** Com a chave informada, sem a propriedade. */
    public AttributeEncryptor(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        return CifraDeAtributo.cifrar(chave(), attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        if (CifraDeAtributo.formatoAtual(dbData)) {
            return CifraDeAtributo.decifrar(chave(), dbData);
        }
        try {
            Cipher cipher = Cipher.getInstance(AES_ECB);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(chave(), AES_ECB));
            return new String(cipher.doFinal(Base64.getDecoder().decode(dbData)));
        } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException
                 | NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new IllegalStateException(e);
        }
    }

    private byte[] chave() {
        byte[] atual = key;
        if (atual == null) {
            if (encryptionKey == null || encryptionKey.isEmpty()) {
                throw new IllegalStateException("aes.encryption.key não está configurada");
            }
            atual = encryptionKey.getBytes(StandardCharsets.UTF_8);
            if (atual.length != 16 && atual.length != 24 && atual.length != 32) {
                throw new IllegalStateException("aes.encryption.key deve ter 16, 24 ou 32 bytes; tem " + atual.length);
            }
            key = atual;
        }
        return atual;
    }
}
