package br.com.archbase.validation.validators;

import br.com.archbase.validation.constraints.EndsWith;
import org.apache.commons.lang3.StringUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.function.BiFunction;

public class EndsWithValidator implements ConstraintValidator<EndsWith, String> {

    protected EndsWith annotation;

    @Override
    public void initialize(EndsWith constraintAnnotation) {
        this.annotation = constraintAnnotation;
    }

    @Override
    @SuppressWarnings("all")
    public boolean isValid(String value, ConstraintValidatorContext context) {
        String[] prefixes = annotation.value();

        BiFunction<String, String, Boolean> fct =
                !annotation.ignoreCase() ?
                        StringUtils::endsWith :
                        StringUtils::endsWithIgnoreCase;

        for (String p : prefixes) {
            if (fct.apply(value, p)) {
                return true;
            }
        }

        return false;
    }
}
