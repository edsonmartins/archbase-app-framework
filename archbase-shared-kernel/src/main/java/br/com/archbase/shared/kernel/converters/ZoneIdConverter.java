package br.com.archbase.shared.kernel.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.ZoneId;

/**
 * @author edsonmartins
 */
@Converter(autoApply = true)
public class ZoneIdConverter implements AttributeConverter<ZoneId, String> {

    @Override
    public String convertToDatabaseColumn(ZoneId attr) {
        return attr == null ? null : attr.toString();
    }

    @Override
    public ZoneId convertToEntityAttribute(String data) {
        return data == null ? null : ZoneId.of(data);
    }

}
