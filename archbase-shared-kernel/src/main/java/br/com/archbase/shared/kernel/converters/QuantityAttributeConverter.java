package br.com.archbase.shared.kernel.converters;

import br.com.archbase.shared.kernel.types.Quantity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;


/**
 * JPA {@link AttributeConverter} para serializar instâncias {@link Quantity} em um {@link Double}. Auto-aplicado a
 * todas as propriedades da entidade do tipo {@link Quantity}.
 *
 * @author edsonmartins
 */
@Converter(autoApply = true)
public class QuantityAttributeConverter implements AttributeConverter<Quantity, Double> {

    @Override
    public Double convertToDatabaseColumn(Quantity quantity) {
        if (quantity == null) {
            return null;
        }

        return quantity.doubleValue();
    }

    @Override
    public Quantity convertToEntityAttribute(Double doubleValue) {
        if (doubleValue == null) {
            return null;
        }
        return new Quantity(doubleValue);
    }

}
