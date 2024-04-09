package br.com.archbase.shared.kernel.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.Year;

/**
 * {@link Year} para inteiro.
 *
 * @author edsonmartins
 */
@Converter(autoApply = true)
public class YearConverter implements AttributeConverter<Year, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Year attr) {
        return attr == null ? null : attr.getValue();
    }

    @Override
    public Year convertToEntityAttribute(Integer data) {
        return data == null ? null : Year.of(data);
    }

}
