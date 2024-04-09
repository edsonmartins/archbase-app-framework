package br.com.archbase.validation.validators;

import br.com.archbase.validation.constraints.LowerCase;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Function;

public class LowerCaseValidator extends GenericStringValidator<LowerCase> {
    @Override
    public Function<String, Boolean> condition() {
        return StringUtils::isAllLowerCase;
    }
}
