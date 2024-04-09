package br.com.archbase.validation.validators;


import br.com.archbase.shared.kernel.utils.ModUtil;
import br.com.archbase.validation.constraints.LuhnCheck;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;

/**
 * Validador de soma de verificação do algoritmo Luhn
 * <p>
 * http://en.wikipedia.org/wiki/Luhn_algorithm
 * http://en.wikipedia.org/wiki/Check_digit
 */
public class LuhnCheckValidator extends ModCheckBase
        implements ConstraintValidator<LuhnCheck, CharSequence> {

    @Override
    public void initialize(LuhnCheck constraintAnnotation) {
        super.initialize(
                constraintAnnotation.startIndex(),
                constraintAnnotation.endIndex(),
                constraintAnnotation.checkDigitIndex(),
                constraintAnnotation.ignoreNonDigitCharacters()
        );
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        return false;
    }

    /**
     * Validar dígito de verificação usando o algoritmo de Luhn
     *
     * @param digits     Os dígitos sobre os quais calcular a soma de verificação
     * @param checkDigit o dígito de verificação
     * @return {@code true} se o resultado da verificação luhn corresponder ao dígito de verificação, {@code false} caso contrário
     */
    @Override
    public boolean isCheckDigitValid(List<Integer> digits, char checkDigit) {
        int modResult = ModUtil.calculateLuhnMod10Check(digits);

        if (!Character.isDigit(checkDigit)) {
            return false;
        }

        int checkValue = extractDigit(checkDigit);
        return checkValue == modResult;
    }
}
