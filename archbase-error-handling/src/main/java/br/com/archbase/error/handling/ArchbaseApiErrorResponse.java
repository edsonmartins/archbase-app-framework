package br.com.archbase.error.handling;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Classe para serialização e retorno dos dados no caso de ocorrência de erros.
 *
 * @author edsonmartins
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ArchbaseApiErrorResponse {
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
    private final Map<String, Object> properties;
    private final List<ArchbaseApiFieldError> fieldErrors;
    private final List<ArchbaseApiGlobalError> globalErrors;

    public ArchbaseApiErrorResponse(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
        this.properties = new HashMap<>();
        this.fieldErrors = new ArrayList<>();
        this.globalErrors = new ArrayList<>();
    }

    @JsonIgnore
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    @JsonAnyGetter
    public Map<String, Object> getProperties() {
        return properties;
    }

    public List<ArchbaseApiFieldError> getFieldErrors() {
        return fieldErrors;
    }

    public List<ArchbaseApiGlobalError> getGlobalErrors() {
        return globalErrors;
    }

    public void addErrorProperties(Map<String, Object> errorProperties) {
        properties.putAll(errorProperties);
    }

    public void addErrorProperty(String propertyName, Object propertyValue) {
        properties.put(propertyName, propertyValue);
    }

    public void addFieldError(ArchbaseApiFieldError fieldError) {
        fieldErrors.add(fieldError);
    }

    public void addGlobalError(ArchbaseApiGlobalError globalError) {
        globalErrors.add(globalError);
    }
}
