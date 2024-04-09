package br.com.archbase.validation.validators;

import br.com.archbase.validation.constraints.OneOfStrings;
import org.apache.commons.lang3.StringUtils;

import jakarta.validation.ConstraintValidatorContext;

public class OneOfStringsValidator extends OneOfValidator<OneOfStrings, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return super.isValid(value, annotation.value(), StringUtils::equals, context);
    }
}
