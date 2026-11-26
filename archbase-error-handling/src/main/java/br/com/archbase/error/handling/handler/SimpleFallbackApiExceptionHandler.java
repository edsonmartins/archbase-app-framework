package br.com.archbase.error.handling.handler;

import br.com.archbase.error.annotations.ArchbaseResponseErrorCode;
import br.com.archbase.error.annotations.ArchbaseResponseErrorProperty;
import br.com.archbase.error.handling.ArchbaseApiErrorResponse;
import br.com.archbase.error.handling.ArchbaseErrorHandlingProperties;
import br.com.archbase.error.handling.FallbackApiExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.ErrorResponse;
import br.com.archbase.error.handling.ArchbaseRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author edsonmartins
 */
@SuppressWarnings("all")
public class SimpleFallbackApiExceptionHandler implements FallbackApiExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleFallbackApiExceptionHandler.class);

    private final ArchbaseErrorHandlingProperties properties;

    public SimpleFallbackApiExceptionHandler(ArchbaseErrorHandlingProperties properties) {
        this.properties = properties;
    }

    @Override
    public ArchbaseApiErrorResponse handle(Throwable exception) {
        HttpStatus statusCode = getHttpStatus(exception);
        String errorCode = getErrorCode(exception);

        ArchbaseApiErrorResponse response = new ArchbaseApiErrorResponse(statusCode, errorCode, getErrorMessage(exception, statusCode));
        response.addErrorProperties(getMethodResponseErrorProperties(exception));
        response.addErrorProperties(getFieldResponseErrorProperties(exception));

        return response;
    }

    /** Texto das respostas 5xx com {@code archbase.error.handling.hide-server-error-messages=true}. */
    static final String MENSAGEM_ERRO_INTERNO = "Erro interno do servidor";

    private String getErrorMessage(Throwable exception, HttpStatus status) {
        if (status.is5xxServerError() && properties.isHideServerErrorMessages()) {
            return MENSAGEM_ERRO_INTERNO;
        }
        if (exception instanceof ErrorResponse errorResponse && errorResponse.getBody().getDetail() != null) {
            // ResponseStatusException e afins: getMessage() vem com o status na frente ("404 NOT_FOUND ...").
            return errorResponse.getBody().getDetail();
        }
        return exception.getMessage();
    }

    private Map<String, Object> getFieldResponseErrorProperties(Throwable exception) {
        Map<String, Object> result = new HashMap<>();
        for (Field field : exception.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(ArchbaseResponseErrorProperty.class)) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(exception);
                    if (value != null || field.getAnnotation(ArchbaseResponseErrorProperty.class).includeIfNull()) {
                        result.put(getPropertyName(field), value);
                    }
                } catch (IllegalAccessException e) {
                    LOGGER.error(String.format("Incapaz de usar o resultado de campo do campo %s.%s", exception.getClass().getName(), field.getName()));
                }
            }
        }
        return result;
    }

    private Map<String, Object> getMethodResponseErrorProperties(Throwable exception) {
        Map<String, Object> result = new HashMap<>();
        Class<? extends Throwable> exceptionClass = exception.getClass();
        for (Method method : exceptionClass.getMethods()) {
            if (method.isAnnotationPresent(ArchbaseResponseErrorProperty.class)
                    && method.getReturnType() != Void.TYPE
                    && method.getParameterCount() == 0) {
                try {
                    method.setAccessible(true);

                    Object value = method.invoke(exception);
                    if (value != null || method.getAnnotation(ArchbaseResponseErrorProperty.class).includeIfNull()) {
                        result.put(getPropertyName(exceptionClass, method),
                                value);
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    LOGGER.error(String.format("Incapaz de usar o resultado do método do método %s.%s", exceptionClass.getName(), method.getName()));
                }
            }
        }
        return result;
    }

    private String getPropertyName(Field field) {
        ArchbaseResponseErrorProperty annotation = AnnotationUtils.getAnnotation(field, ArchbaseResponseErrorProperty.class);
        assert annotation != null;
        if (!StringUtils.isEmpty(annotation.value())) {
            return annotation.value();
        }

        return field.getName();
    }

    private String getPropertyName(Class<? extends Throwable> exceptionClass, Method method) {
        ArchbaseResponseErrorProperty annotation = AnnotationUtils.getAnnotation(method, ArchbaseResponseErrorProperty.class);
        assert annotation != null;
        if (!StringUtils.isEmpty(annotation.value())) {
            return annotation.value();
        }

        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(exceptionClass);
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                if (propertyDescriptor.getReadMethod().equals(method)) {
                    return propertyDescriptor.getName();
                }
            }
        } catch (IntrospectionException e) {
            //ignore
        }

        return method.getName();
    }

    /**
     * Na ordem: o status que a própria exceção traz ({@link ErrorResponse} do Spring — {@code ResponseStatusException},
     * parâmetro obrigatório ausente, validação de método — e {@link ArchbaseRuntimeException#getStatusCode()}); o
     * {@code @ResponseStatus} da classe ou de uma superclasse; senão 500.
     *
     * <p>Até a 3.5.0 só o {@code @ResponseStatus} da própria classe contava, e todos esses casos respondiam 500.
     */
    private HttpStatus getHttpStatus(Throwable exception) {
        if (exception instanceof ErrorResponse errorResponse) {
            HttpStatus status = HttpStatus.resolve(errorResponse.getStatusCode().value());
            if (status != null) {
                return status;
            }
        }
        if (exception instanceof ArchbaseRuntimeException archbase && archbase.getStatusCode() != null) {
            return archbase.getStatusCode();
        }
        ResponseStatus responseStatus = AnnotatedElementUtils.findMergedAnnotation(exception.getClass(), ResponseStatus.class);
        return responseStatus != null ? responseStatus.code() : HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String getErrorCode(Throwable exception) {
        ArchbaseResponseErrorCode errorCodeAnnotation = AnnotationUtils.getAnnotation(exception.getClass(), ArchbaseResponseErrorCode.class);
        String code;
        if (errorCodeAnnotation != null) {
            code = errorCodeAnnotation.value();
        } else {
            code = exception.getClass().getName();
        }

        return replaceCodeWithConfiguredOverrideIfPresent(code);
    }

    private String replaceCodeWithConfiguredOverrideIfPresent(String code) {
        return properties.getCodes().getOrDefault(code, code);
    }

}
