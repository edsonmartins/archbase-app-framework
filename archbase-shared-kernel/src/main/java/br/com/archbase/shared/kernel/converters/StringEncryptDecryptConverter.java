package br.com.archbase.shared.kernel.converters;

import jakarta.persistence.Converter;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Implementação concreta do {@link AbstractEncryptDecryptConverter}
 * classe abstrata para criptografar / descriptografar um atributo de entidade do tipo
 * {@link String} <br/>
 * Observação: esta é a classe em que a anotação do conversor {@literal @} é aplicada
 *
 * @author edsonmartins
 */
@Converter(autoApply = false)
public class StringEncryptDecryptConverter
        extends AbstractEncryptDecryptConverter<String> {

    /**
     * O construtor padrão inicializa com uma instância do
     * Classe de criptografia {@link CipherMaker} para obter um {@link jakarta.crypto.Cipher}
     * instância
     */
    public StringEncryptDecryptConverter() {
        this(new CipherMaker());
    }

    /**
     * Constructor
     *
     * @param cipherMaker
     */
    public StringEncryptDecryptConverter(CipherMaker cipherMaker) {
        super(cipherMaker);
    }

    @Override
    boolean isNotNullOrEmpty(String attribute) {
        return isNotEmpty(attribute);
    }

    @Override
    String convertStringToEntityAttribute(String dbData) {
        // a entrada é uma string e a saída é uma string
        return dbData;
    }

    @Override
    String convertEntityAttributeToString(String attribute) {
        // Aqui também a entrada é uma string e a saída é uma string
        return attribute;
    }
}
