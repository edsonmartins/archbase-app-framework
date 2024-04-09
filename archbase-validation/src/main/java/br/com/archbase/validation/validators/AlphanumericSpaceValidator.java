package br.com.archbase.validation.validators;

import br.com.archbase.validation.constraints.AlphanumericSpace;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Function;

public class AlphanumericSpaceValidator extends GenericStringValidator<AlphanumericSpace> {
    @Override
    public Function<String, Boolean> condition() {
        return StringUtils::isAlphanumericSpace;
    }
}
