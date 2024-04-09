package br.com.archbase.validation.validators;

import br.com.archbase.validation.fluentvalidator.exception.ArchbaseValidationException;
import br.com.caelum.stella.MessageProducer;
import br.com.caelum.stella.SimpleValidationMessage;
import br.com.caelum.stella.ValidationMessage;
import br.com.caelum.stella.validation.InvalidValue;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Recupera mensagens de validação definida na anotação do Hibernate
 * Validator.
 */
public class AnnotationMessageProducer implements MessageProducer {

    private final Annotation constraint;

    public AnnotationMessageProducer(final Annotation constraint) {
        this.constraint = constraint;
    }

    /**
     * Este método sempre retornará a mesma ValidationMessage, pois o Hibernate * Validator só deixa uma mensagem por
     * Validator, definida dentro da * anotação de restrição.
     *
     * @param invalidValue será ignorado
     * @return a mensagem definida pela anotação de restrição relacionada
     */
    @SuppressWarnings("java:S3011")
    public ValidationMessage getMessage(final InvalidValue invalidValue) {
        try {
            Method constraintMessage = constraint.annotationType().getDeclaredMethod("message");
            constraintMessage.setAccessible(true);
            String message = constraintMessage.invoke(constraint).toString();
            return new SimpleValidationMessage(message);
        } catch (NoSuchMethodException e) {
            // mesmo comportamento dos validadores embutidos do Hibernate Validator
            // veja org.hibernate.validator.interpolator.DefaultMessageInterpolator
            throw new IllegalArgumentException("Annotation " + constraint
                    + " não tem um atributo de mensagem (acessível)");
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ArchbaseValidationException(e) {
            };

        }
    }
}
