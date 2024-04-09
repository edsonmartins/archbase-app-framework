package br.com.archbase.shared.kernel.converters;

import jakarta.persistence.AttributeConverter;
import java.time.Duration;

/**
 * {@link Duration} para nano seconds
 *
 * @author edsonmartins
 */
public class DurationToNanoConverter implements AttributeConverter<Duration, Long> {

    @Override
    public Long convertToDatabaseColumn(Duration attr) {
        return attr == null ? null : attr.toNanos();
    }

    @Override
    public Duration convertToEntityAttribute(Long data) {
        return data == null ? null : Duration.ofNanos(data);
    }

}
