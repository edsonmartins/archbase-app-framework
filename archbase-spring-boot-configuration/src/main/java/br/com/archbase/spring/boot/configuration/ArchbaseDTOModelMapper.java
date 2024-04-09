package br.com.archbase.spring.boot.configuration;

import br.com.archbase.ddd.dto.annotations.RequestBodyDTO;
import br.com.archbase.ddd.dto.annotations.ResponseBodyDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collections;

public class ArchbaseDTOModelMapper extends RequestResponseBodyMethodProcessor {
    private static final ModelMapper modelMapper = new ModelMapper();

    private EntityManager entityManager;

    public ArchbaseDTOModelMapper(ObjectMapper objectMapper, EntityManager entityManager) {
        super(Collections.singletonList(new MappingJackson2HttpMessageConverter(objectMapper)));
        this.entityManager = entityManager;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(RequestBodyDTO.class);
    }

    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        return returnType.hasParameterAnnotation(ResponseBodyDTO.class);
    }

    @Override
    protected void validateIfApplicable(WebDataBinder binder, MethodParameter parameter) {
        binder.validate();
    }

    @Override
    public void handleReturnValue(Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws IOException, HttpMediaTypeNotAcceptableException {
        ResponseBodyDTO methodAnnotation = returnType.getMethodAnnotation(ResponseBodyDTO.class);
        if (methodAnnotation != null && returnValue != null) {
            mavContainer.setRequestHandled(true);
            ServletServerHttpRequest inputMessage = createInputMessage(webRequest);
            ServletServerHttpResponse outputMessage = createOutputMessage(webRequest);

            Object result = modelMapper.map(returnValue, methodAnnotation.value());

            // Tente mesmo com valor de retorno nulo. ResponseBodyAdvice pode ser envolvido.
            writeWithMessageConverters(result, returnType, inputMessage, outputMessage);
        } else {
            super.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
        }
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        Object dto = super.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
        Object id = getEntityId(dto);
        if (id == null) {
            return modelMapper.map(dto, parameter.getParameterType());
        } else {
            Object persistedObject = entityManager.find(parameter.getParameterType(), id);
            modelMapper.map(dto, persistedObject);
            return persistedObject;
        }
    }

    @Override
    protected Object readWithMessageConverters(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType) throws IOException, HttpMediaTypeNotSupportedException {
        for (Annotation ann : parameter.getParameterAnnotations()) {
            RequestBodyDTO requestBodyDtoType = AnnotationUtils.getAnnotation(ann, RequestBodyDTO.class);
            if (requestBodyDtoType != null) {
                return super.readWithMessageConverters(inputMessage, parameter, requestBodyDtoType.value());
            }
        }
        throw new ArchbaseConfigurationException();
    }

    @SuppressWarnings("java:S3011")
    private Object getEntityId(@NotNull Object dto) {
        for (Field field : dto.getClass().getDeclaredFields()) {
            if (field.getAnnotation(Id.class) != null) {
                try {
                    field.setAccessible(true);
                    return field.get(dto);
                } catch (IllegalAccessException e) {
                    throw new ArchbaseConfigurationException(e);
                }
            }
        }
        return null;
    }
}