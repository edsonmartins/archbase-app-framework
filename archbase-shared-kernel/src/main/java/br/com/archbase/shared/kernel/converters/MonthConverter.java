package br.com.archbase.shared.kernel.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.Month;

/**
 * {@link Month} para inteiro.
 *
 * @author edsonmartins
 */
@Converter(autoApply = true)
public class MonthConverter implements AttributeConverter<Month, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Month attr) {
        return attr == null ? null : attr.getValue();
    }

    @Override
    public Month convertToEntityAttribute(Integer data) {
        return data == null ? null : Month.of(data);
    }

}
