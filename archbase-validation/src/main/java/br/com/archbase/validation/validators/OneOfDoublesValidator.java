package br.com.archbase.validation.validators;

import br.com.archbase.validation.constraints.OneOfDoubles;
import org.apache.commons.lang3.ArrayUtils;

import jakarta.validation.ConstraintValidatorContext;
import java.util.Objects;

public class OneOfDoublesValidator extends OneOfValidator<OneOfDoubles, Double> {

    @Override
    public boolean isValid(Double value, ConstraintValidatorContext context) {
        return super.isValid(value, ArrayUtils.toObject(annotation.value()), Objects::equals, context);
    }

}
