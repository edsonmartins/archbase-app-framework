package br.com.archbase.shared.kernel.converters;

import br.com.archbase.shared.kernel.types.Description;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;


/**
 * JPA {@link AttributeConverter} para serializar instâncias {@link MonetaryAmount} em um {@link String}. Auto-aplicado a
 * todas as propriedades da entidade do tipo {@link Description}.
 *
 * @author edsonmartins
 */
@Converter(autoApply = true)
public class DescriptionAttributeConverter implements AttributeConverter<Description, String> {

    @Override
    public String convertToDatabaseColumn(Description description) {
        if (description == null) {
            return null;
        }
        return description.getValue();
    }

    @Override
    public Description convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        return Description.of(value);
    }

}
