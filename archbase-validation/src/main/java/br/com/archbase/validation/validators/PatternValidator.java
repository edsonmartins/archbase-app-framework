package br.com.archbase.validation.validators;


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * validador usando uma expressão regular,
 * com base na anotação de restrição do padrão jsr303.
 */
public class PatternValidator implements ConstraintValidator<Pattern, String> {
    protected java.util.regex.Pattern pattern;

    @Override
    public void initialize(Pattern annotation) {
        Pattern.Flag[] flags = annotation.flags();
        int intFlag = 0;
        for (Pattern.Flag flag : flags) {
            intFlag = intFlag | flag.getValue();
        }

        try {
            pattern = java.util.regex.Pattern.compile(annotation.regexp(), intFlag);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Expressão regular inválida.", e);
        }
    }


    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || pattern.matcher(value).matches();
    }
}
