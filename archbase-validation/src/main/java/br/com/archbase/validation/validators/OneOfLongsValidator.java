package br.com.archbase.validation.validators;

import br.com.archbase.shared.kernel.utils.ArrayUtils;
import br.com.archbase.validation.constraints.OneOfLongs;

import jakarta.validation.ConstraintValidatorContext;
import java.util.Objects;

public class OneOfLongsValidator extends OneOfValidator<OneOfLongs, Long> {

    @Override
    public boolean isValid(Long value, ConstraintValidatorContext context) {
        return super.isValid(value, ArrayUtils.toObject(annotation.value()), Objects::equals, context);
    }
}
