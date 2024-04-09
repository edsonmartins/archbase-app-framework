package br.com.archbase.validation.validators;

import br.com.archbase.validation.constraints.Numeric;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Function;

public class NumericValidator extends GenericStringValidator<Numeric> {
    @Override
    public Function<String, Boolean> condition() {
        return StringUtils::isNumeric;
    }
}
