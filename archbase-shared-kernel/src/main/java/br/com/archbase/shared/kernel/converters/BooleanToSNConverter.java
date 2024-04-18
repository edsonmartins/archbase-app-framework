package br.com.archbase.shared.kernel.converters;


import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class BooleanToSNConverter implements AttributeConverter<Boolean, String> {

    @Override
    public String convertToDatabaseColumn(Boolean value) {
        return (value != null && value) ? "S" : "N";
    }

    @Override
    public Boolean convertToEntityAttribute(String value) {
        return "S".equals(value);
    }
}