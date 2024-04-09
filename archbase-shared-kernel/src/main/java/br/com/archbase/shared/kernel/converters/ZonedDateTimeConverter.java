package br.com.archbase.shared.kernel.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Converte {@link ZonedDateTime} para {@link Timestamp} e vice-versa. <br>
 * NOTA: podemos perder a diferença de fuso horário com algum banco de dados; por exemplo. PostgreSQL
 *
 * @author edsonmartins
 * @see <a href="https://github.com/marschall/threeten-jpa">ThreeTen JPA</a>
 * @see <a href="https://bitbucket.org/montanajava/jpaattributeconverters">Using the Java 8 Date Time Classes with JPA!</a>
 */
@Converter(autoApply = true)
public class ZonedDateTimeConverter implements AttributeConverter<ZonedDateTime, Timestamp> {

    @Override
    public Timestamp convertToDatabaseColumn(ZonedDateTime attr) {
        return attr == null ? null : Timestamp.from(attr.toInstant());
    }

    @Override
    public ZonedDateTime convertToEntityAttribute(Timestamp data) {
        return data == null ? null : data.toInstant().atZone(ZoneId.systemDefault());
    }
}
