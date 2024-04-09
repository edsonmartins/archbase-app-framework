package br.com.archbase.validation.validators;

import br.com.archbase.shared.kernel.bean.metadata.BeanDescriptor;
import br.com.archbase.validation.constraints.NotNullIfAnotherFieldHasValue;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementação do validador {@link NotNullIfAnotherFieldHasValue}.
 */
public class NotNullIfAnotherFieldHasValueValidator
        implements ConstraintValidator<NotNullIfAnotherFieldHasValue, Object> {

    private String fieldName;

    private String expectedFieldValue;

    private String[] dependFieldName;

    @Override
    public void initialize(NotNullIfAnotherFieldHasValue annotation) {
        fieldName = annotation.fieldName();
        expectedFieldValue = annotation.fieldValue();
        dependFieldName = annotation.dependFieldName();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        BeanDescriptor descriptor = new BeanDescriptor(value.getClass());
        descriptor.getValue(fieldName);
        Object fieldValue = descriptor.getValue(fieldName);
        for (String dpFld : dependFieldName) {
            Object dependFieldValue = descriptor.getValue(dpFld);
            if (fieldValue != null && fieldValue.toString().equals(expectedFieldValue) && dependFieldValue == null) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                        .addPropertyNode(dpFld).addConstraintViolation();
                return false;
            }
        }

        return true;
    }

}
