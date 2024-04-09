package br.com.archbase.resource.logger.bean;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author edsonmartins
 */
public class RequestContext {

    private final Map<String, String> context = new LinkedHashMap<>(4);

    public RequestContext() {

    }

    public RequestContext(@Nonnull Map<String, String> context) {
        context.forEach((key, value) -> this.context.put(key, value != null ? value : "null"));
    }

    public RequestContext add(@Nonnull String key, @Nullable String value) {
        context.put(key, value != null ? value : "null");
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RequestContext that = (RequestContext) o;

        return context.equals(that.context);
    }

    @Override
    public int hashCode() {
        return context.hashCode();
    }

    @Override
    public String toString() {
        return context.entrySet().stream()
                .map(e -> e.getKey() + ": [" + e.getValue() + "]")
                .collect(Collectors.joining(", "));
    }
}
