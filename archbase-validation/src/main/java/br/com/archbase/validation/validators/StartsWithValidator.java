package br.com.archbase.validation.validators;

import br.com.archbase.validation.constraints.StartsWith;
import org.apache.commons.lang3.StringUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.function.BiFunction;

@SuppressWarnings("all")
public class StartsWithValidator implements ConstraintValidator<StartsWith, String> {

    protected StartsWith annotation;

    @Override
    public void initialize(StartsWith constraintAnnotation) {
        this.annotation = constraintAnnotation;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        String[] prefixes = annotation.value();

        BiFunction<String, String, Boolean> fct =
                !annotation.ignoreCase() ?
                        StringUtils::startsWith :
                        StringUtils::startsWithIgnoreCase;

        for (String p : prefixes) {
            if (fct.apply(value, p)) {
                return true;
            }
        }

        return false;
    }
}
