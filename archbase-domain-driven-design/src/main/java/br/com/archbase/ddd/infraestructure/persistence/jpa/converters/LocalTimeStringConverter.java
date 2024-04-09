package br.com.archbase.ddd.infraestructure.persistence.jpa.converters;


import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Converte {@link LocalTime} de e em strings do formato 'HH:MM'
 */
@Converter(autoApply = true)
public class LocalTimeStringConverter implements AttributeConverter<LocalTime, String> {

    public static final DateTimeFormatter DEFAULT_TIME_PARSER = DateTimeFormatter.ISO_LOCAL_TIME;
    public static final DateTimeFormatter DEFAULT_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final String PART_DELIMITER = ":";
    private DateTimeFormatter parser;
    private DateTimeFormatter formatter;

    public LocalTimeStringConverter() {
        this(DEFAULT_TIME_PARSER, DEFAULT_TIME_FORMATTER);
    }

    public LocalTimeStringConverter(DateTimeFormatter parser, DateTimeFormatter formatter) {
        this.parser = parser;
        this.formatter = formatter;
    }

    @Override
    public String convertToDatabaseColumn(LocalTime attribute) {
        return formatter.format(attribute);
    }

    @Override
    public LocalTime convertToEntityAttribute(String dbData) {
        try {
            if (dbData.length() == 1) {
                dbData = "0" + dbData;
            }
            if (!dbData.contains(PART_DELIMITER)) {
                dbData = dbData + PART_DELIMITER + "00";
            }

            return LocalTime.parse(dbData, parser);
        } catch (Exception e) {
            return null;
        }
    }
}
