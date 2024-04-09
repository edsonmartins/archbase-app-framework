package br.com.archbase.validation.validators;



import br.com.archbase.shared.kernel.utils.ArrayUtils;
import br.com.archbase.validation.constraints.OneOfIntegers;

import jakarta.validation.ConstraintValidatorContext;
import java.util.Objects;

public class OneOfIntegersValidator extends OneOfValidator<OneOfIntegers, Integer> {

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        return super.isValid(value, ArrayUtils.toObject(annotation.value()), Objects::equals, context);
    }
}
