package br.com.archbase.validation.validators;


import br.com.archbase.validation.constraints.Alphanumeric;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Function;

public class AlphanumericValidator extends GenericStringValidator<Alphanumeric> {
    @Override
    public Function<String, Boolean> condition() {
        return StringUtils::isAlphanumeric;
    }
}