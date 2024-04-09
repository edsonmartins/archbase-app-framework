package br.com.archbase.validation.validators;

import br.com.archbase.shared.kernel.utils.ModUtil;
import br.com.archbase.validation.constraints.ModCheck;
import br.com.archbase.validation.constraints.ModCheck.ModType;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;

/**
 * Validador de verificação de mod para algoritmos MOD10 e MOD11
 * <p>
 * http://en.wikipedia.org/wiki/Luhn_algorithm
 * http://en.wikipedia.org/wiki/Check_digit
 */
public class ModCheckValidator extends ModCheckBase implements ConstraintValidator<ModCheck, CharSequence> {
    /**
     * Multiplicador usado pelos algoritmos mod
     */
    private int multiplier;

    /**
     * O tipo de algoritmo de soma de verificação
     */
    private ModType modType;


    @Override
    public void initialize(ModCheck constraintAnnotation) {
        super.initialize(
                constraintAnnotation.startIndex(),
                constraintAnnotation.endIndex(),
                constraintAnnotation.checkDigitPosition(),
                constraintAnnotation.ignoreNonDigitCharacters()
        );

        this.modType = constraintAnnotation.modType();
        this.multiplier = constraintAnnotation.multiplier();
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        return false;
    }

    /**
     * Verifique se a entrada passa no teste Mod10 (apenas implementação do algoritmo Luhn) ou Mod11
     *
     * @param digits     os dígitos sobre os quais calcular a soma de verificação Mod10 ou Mod11
     * @param checkDigit o dígito de verificação
     * @return {@code true} se o resultado mod 10/11 corresponder ao dígito de verificação, {@code false} caso contrário
     */
    @Override
    public boolean isCheckDigitValid(List<Integer> digits, char checkDigit) {
        int modResult = -1;
        int checkValue = extractDigit(checkDigit);

        if (modType.equals(ModType.MOD11)) {
            modResult = ModUtil.calculateMod11Check(digits, multiplier);

            if (modResult == 10 || modResult == 11) {
                modResult = 0;
            }
        } else {
            modResult = ModUtil.calculateLuhnMod10Check(digits);
        }

        return checkValue == modResult;
    }

}
