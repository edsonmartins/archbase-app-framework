package br.com.archbase.shared.kernel.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.Duration;

/**
 * @author edson martins
 */
@Converter(autoApply = true)
public class DurationToStringConverter implements AttributeConverter<Duration, String> {

    @Override
    public String convertToDatabaseColumn(Duration attr) {
        return attr == null ? null : attr.toString();
    }

    @Override
    public Duration convertToEntityAttribute(String data) {
        return data == null ? null : Duration.parse(data);
    }

}
