package br.com.archbase.validation.validators;

import br.com.archbase.validation.constraints.UpperCase;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Function;

public class UpperCaseValidator extends GenericStringValidator<UpperCase> {

    @Override
    public Function<String, Boolean> condition() {
        return StringUtils::isAllUpperCase;
    }

}
