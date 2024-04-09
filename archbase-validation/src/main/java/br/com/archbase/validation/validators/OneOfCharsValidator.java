package br.com.archbase.validation.validators;

import br.com.archbase.shared.kernel.utils.ArrayUtils;
import br.com.archbase.validation.constraints.OneOfChars;

import jakarta.validation.ConstraintValidatorContext;
import java.util.Objects;

public class OneOfCharsValidator extends OneOfValidator<OneOfChars, Character> {
    @Override
    public boolean isValid(Character value, ConstraintValidatorContext context) {
        return super.isValid(value, ArrayUtils.toObject(annotation.value()), Objects::equals, context);
    }
}
