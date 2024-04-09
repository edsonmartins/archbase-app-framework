package br.com.archbase.validation.validators;

import br.com.archbase.validation.constraints.Alpha;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Function;


public class AlphaValidator extends GenericStringValidator<Alpha> {
    @Override
    public Function<String, Boolean> condition() {
        return StringUtils::isAlphanumeric;
    }
}