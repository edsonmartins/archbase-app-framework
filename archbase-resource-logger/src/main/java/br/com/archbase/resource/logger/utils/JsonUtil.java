package br.com.archbase.resource.logger.utils;

import br.com.archbase.resource.logger.exceptions.ArchbaseResourceLoggerException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Type;

/**
 * @author edsonmartins
 */
public class JsonUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonUtil() {
    }

    @Nonnull
    public static String toJson(@Nullable Object object) {
        try {
            return object == null ? "null" : OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new ArchbaseResourceLoggerException(e);
        }
    }

    public static <T> T fromJson(@Nonnull String json, @Nonnull Type type) {
        JavaType javaType = OBJECT_MAPPER.constructType(type);
        try {
            return OBJECT_MAPPER.readValue(json, javaType);
        } catch (IOException e) {
            throw new ArchbaseResourceLoggerException(e);
        }
    }
}
