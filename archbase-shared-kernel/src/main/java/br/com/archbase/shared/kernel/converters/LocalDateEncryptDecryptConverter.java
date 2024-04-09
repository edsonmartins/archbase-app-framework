package br.com.archbase.shared.kernel.converters;

import jakarta.persistence.Converter;
import java.time.LocalDate;

import static java.time.format.DateTimeFormatter.ISO_DATE;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Implementação concreta do {@link AbstractEncryptDecryptConverter}
 * classe abstrata para criptografar / descriptografar um atributo de entidade do tipo
 * {@link LocalDate}<br/>
 * Observação: esta é a classe em que a anotação do conversor {@literal @} é aplicada
 *
 * @author edsonmartins
 */
@Converter(autoApply = false)
public class LocalDateEncryptDecryptConverter
        extends AbstractEncryptDecryptConverter<LocalDate> {

    public LocalDateEncryptDecryptConverter() {
        this(new CipherMaker());
    }

    public LocalDateEncryptDecryptConverter(CipherMaker cipherMaker) {
        super(cipherMaker);
    }

    @Override
    boolean isNotNullOrEmpty(LocalDate attribute) {
        return attribute != null;
    }

    @Override
    LocalDate convertStringToEntityAttribute(String dbData) {
        return isEmpty(dbData) ? null : LocalDate.parse(dbData, ISO_DATE);
    }

    @Override
    String convertEntityAttributeToString(LocalDate attribute) {
        return ((attribute == null) ? null : attribute.format(ISO_DATE));
    }
}