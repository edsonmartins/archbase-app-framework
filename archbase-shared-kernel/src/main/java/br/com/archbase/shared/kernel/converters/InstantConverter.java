package br.com.archbase.shared.kernel.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * Converte {@link Instant} para {@link Timestamp} e vice-versa.
 *
 * @author edsonmartins
 * @see <a href="http://www.thoughts-on-java.org/persist-localdate-Instant-jpa">Java 8 Instant in JPA</a>
 */
@Converter(autoApply = true)
public class InstantConverter implements AttributeConverter<Instant, Timestamp> {

    @Override
    public Timestamp convertToDatabaseColumn(Instant attr) {
        return attr == null ? null : Timestamp.from(attr);
    }

    @Override
    public Instant convertToEntityAttribute(Timestamp data) {
        return data == null ? null : data.toInstant();
    }
}
