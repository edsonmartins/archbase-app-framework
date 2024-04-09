package br.com.archbase.shared.kernel.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.DayOfWeek;

/**
 * {@link DayOfWeek} para inteiro.
 *
 * @author edsonmartins
 */
@Converter(autoApply = true)
public class DayOfWeekConverter implements AttributeConverter<DayOfWeek, Integer> {

    @Override
    public Integer convertToDatabaseColumn(DayOfWeek attr) {
        return attr == null ? null : attr.getValue();
    }

    @Override
    public DayOfWeek convertToEntityAttribute(Integer data) {
        return data == null ? null : DayOfWeek.of(data);
    }

}
