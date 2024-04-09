package br.com.archbase.validation.validators;

import br.com.archbase.shared.kernel.bean.metadata.BeanDescriptor;
import br.com.archbase.validation.constraints.NotNullIfAnotherFieldHasValue;
import br.com.archbase.validation.constraints.NullIfAnotherFieldHasValue;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementação do validador {@link NotNullIfAnotherFieldHasValue}.
 */
public class NullIfAnotherFieldHasValueValidator
        implements ConstraintValidator<NullIfAnotherFieldHasValue, Object> {

    private String fieldName;

    private String[] dependFieldName;

    @Override
    public void initialize(NullIfAnotherFieldHasValue annotation) {
        fieldName = annotation.fieldName();
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

            if (fieldValue != null && dependFieldValue != null) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                        .addPropertyNode(dpFld).addConstraintViolation();

                return false;
            }
        }

        return true;
    }

}
