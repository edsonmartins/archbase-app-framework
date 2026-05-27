package br.com.archbase.resource.logger.utils;

import br.com.archbase.resource.logger.exceptions.ArchbaseResourceLoggerException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.lang.reflect.Type;

/**
 * @author edsonmartins
 */
public class JsonUtil {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    private JsonUtil() {
    }

    @Nonnull
    public static String toJson(@Nullable Object object) {
        try {
            return object == null ? "null" : OBJECT_MAPPER.writeValueAsString(object);
        } catch (JacksonException e) {
            throw new ArchbaseResourceLoggerException(e);
        }
    }

    public static <T> T fromJson(@Nonnull String json, @Nonnull Type type) {
        JavaType javaType = OBJECT_MAPPER.constructType(type);
        try {
            return OBJECT_MAPPER.readValue(json, javaType);
        } catch (JacksonException e) {
            throw new ArchbaseResourceLoggerException(e);
        }
    }
}
