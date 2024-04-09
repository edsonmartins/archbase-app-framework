package br.com.archbase.validation.validators;


import br.com.archbase.validation.constraints.AlphaSpace;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Function;

public class AlphaSpaceValidator extends GenericStringValidator<AlphaSpace> {
    @Override
    public Function<String, Boolean> condition() {
        return StringUtils::isAlphaSpace;
    }
}
