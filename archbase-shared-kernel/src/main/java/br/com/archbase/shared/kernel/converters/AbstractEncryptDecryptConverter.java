package br.com.archbase.shared.kernel.converters;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import jakarta.persistence.AttributeConverter;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Classe base abstrata para implementar o conversor de atributos JPA que irá
 * criptografar e descriptografar um atributo de entidade (coluna da tabela)
 *
 * @author edsonmartins
 */
public abstract class AbstractEncryptDecryptConverter<X>
        implements AttributeConverter<X, String> {

    /**
     * Esta é a chave necessária para criptografar / descriptografar. Isso é definido aqui
     * por exemplo, propósito. Na produção, isso deve vir de um ambiente seguro
     * localização não acessível facilmente. No Spring Boot, uma localização possível é
     * o arquivo application.properties. Embora não seja a maneira mais segura,
     * manterá esta chave fora do código Java real.
     */
    private static final String SECRET_ENCRYPTION_KEY = "MySuperSecretKey";

    /**
     * CipherMaker é necessário para configurar e criar instância de Cipher
     */
    private CipherMaker cipherMaker;

    /**
     * Constructor
     *
     * @param cipherMaker
     */
    public AbstractEncryptDecryptConverter(CipherMaker cipherMaker) {
        this.cipherMaker = cipherMaker;
    }

    /**
     * Constructor padrão
     */
    public AbstractEncryptDecryptConverter() {
        this(new CipherMaker());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * jakarta.persistence.AttributeConverter#convertToDatabaseColumn(java.lang.
     * Object)
     */
    @Override
    public String convertToDatabaseColumn(X attribute) {
        if (isNotEmpty(SECRET_ENCRYPTION_KEY) && isNotNullOrEmpty(attribute)) {
            try {
                Cipher cipher = cipherMaker.configureAndGetInstance(
                        Cipher.ENCRYPT_MODE,
                        SECRET_ENCRYPTION_KEY);
                return encryptData(cipher, attribute);
            } catch (NoSuchAlgorithmException
                     | InvalidKeyException
                     | InvalidAlgorithmParameterException
                     | BadPaddingException
                     | NoSuchPaddingException
                     | IllegalBlockSizeException e) {
                throw new RuntimeException(e);
            }
        }
        return convertEntityAttributeToString(attribute);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * jakarta.persistence.AttributeConverter#convertToEntityAttribute(java.lang.
     * Object)
     */
    @Override
    public X convertToEntityAttribute(String dbData) {
        if (isNotEmpty(SECRET_ENCRYPTION_KEY) && isNotEmpty(dbData)) {
            try {
                Cipher cipher = cipherMaker.configureAndGetInstance(
                        Cipher.DECRYPT_MODE,
                        SECRET_ENCRYPTION_KEY);
                return decryptData(cipher, dbData);
            } catch (NoSuchAlgorithmException
                     | InvalidAlgorithmParameterException
                     | InvalidKeyException
                     | NoSuchPaddingException
                     | BadPaddingException
                     | IllegalBlockSizeException e) {
                throw new RuntimeException(e);
            }
        }
        return convertStringToEntityAttribute(dbData);
    }

    /**
     * A classe concreta que implementa esta classe abstrata terá que
     * fornecer a implementação. Para criptografia de string simples, o
     * a implementação é simples porque o apache commons lang StringUtils pode ser usado.
     * Mas este método foi abstraído porque pode haver outros tipos de nulos
     * técnica de verificação necessária quando uma entidade não String deve ser criptografada
     *
     * @param attribute
     * @return
     */
    abstract boolean isNotNullOrEmpty(X attribute);

    /**
     * A classe concreta que implementa esta classe abstrata terá que
     * fornecer a implementação. Para descriptografar uma string, é simples como um
     * String deve ser retornada, mas para outros tipos não String mais algum código
     * pode ter que ser implementado. Por exemplo, um tipo de data de string de data.
     *
     * @param dbData
     * @return
     */
    abstract X convertStringToEntityAttribute(String dbData);

    /**
     * A classe concreta que implementa esta classe abstrata terá que
     * fornecer a implementação. Para criptografar uma string, é simples como um
     * String deve ser retornada, mas para outros tipos não String mais algum código
     * pode ter que ser implementado. Por exemplo, um tipo de data de string de data.
     *
     * @param attribute
     * @return
     */
    abstract String convertEntityAttributeToString(X attribute);

    /**
     * Método auxiliar para criptografar dados
     *
     * @param cipher
     * @param attribute
     * @return
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    private String encryptData(Cipher cipher, X attribute)
            throws IllegalBlockSizeException, BadPaddingException {
        byte[] bytesToEncrypt = convertEntityAttributeToString(attribute)
                .getBytes();
        byte[] encryptedBytes = cipher.doFinal(bytesToEncrypt);
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * Método auxiliar para descriptografar dados
     *
     * @param cipher
     * @param dbData
     * @return
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    private X decryptData(Cipher cipher, String dbData)
            throws IllegalBlockSizeException, BadPaddingException {
        byte[] bytesToDecrypt = Base64.getDecoder().decode(dbData);
        byte[] decryptedBytes = cipher.doFinal(bytesToDecrypt);
        return convertStringToEntityAttribute(new String(decryptedBytes));
    }
}