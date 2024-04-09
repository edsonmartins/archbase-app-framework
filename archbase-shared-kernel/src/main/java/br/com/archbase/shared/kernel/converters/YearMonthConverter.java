package br.com.archbase.shared.kernel.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.YearMonth;

/**
 * @author edsonmartins
 */
@Converter(autoApply = true)
public class YearMonthConverter implements AttributeConverter<YearMonth, String> {

    @Override
    public String convertToDatabaseColumn(YearMonth attr) {
        return attr == null ? null : attr.toString();
    }

    @Override
    public YearMonth convertToEntityAttribute(String data) {
        return data == null ? null : YearMonth.parse(data);
    }

}
