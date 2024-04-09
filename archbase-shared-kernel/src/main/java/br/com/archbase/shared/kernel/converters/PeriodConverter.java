package br.com.archbase.shared.kernel.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.Period;

/**
 * @author edsonmartins
 */
@Converter(autoApply = true)
public class PeriodConverter implements AttributeConverter<Period, String> {

    @Override
    public String convertToDatabaseColumn(Period attr) {
        return attr == null ? null : attr.toString();
    }

    @Override
    public Period convertToEntityAttribute(String data) {
        return data == null ? null : Period.parse(data);
    }

}
