package br.com.archbase.validation.validators;

import br.com.archbase.shared.kernel.utils.ModUtil;
import br.com.archbase.validation.constraints.Mod11Check;
import br.com.archbase.validation.constraints.Mod11Check.ProcessingDirection;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Collections;
import java.util.List;

/**
 * Validador de dígito de verificação Mod11
 * <p>
 * http://en.wikipedia.org/wiki/Check_digit
 */
public class Mod11CheckValidator extends ModCheckBase
        implements ConstraintValidator<Mod11Check, CharSequence> {

    private boolean reverseOrder;

    /**
     * O {@code char} que representa o dígito de verificação quando mod11
     * checksum igual a 10.
     */
    private char treatCheck10As;

    /**
     * O {@code char} que representa o dígito de verificação quando mod11
     * checksum igual a 10.
     */
    private char treatCheck11As;

    /**
     * @return O limite para o crescimento do multiplicador do multiplicador do algoritmo
     */
    private int threshold;

    @Override
    public void initialize(Mod11Check constraintAnnotation) {
        super.initialize(
                constraintAnnotation.startIndex(),
                constraintAnnotation.endIndex(),
                constraintAnnotation.checkDigitIndex(),
                constraintAnnotation.ignoreNonDigitCharacters()
        );
        this.threshold = constraintAnnotation.threshold();

        this.reverseOrder = constraintAnnotation.processingDirection() == ProcessingDirection.LEFT_TO_RIGHT;

        this.treatCheck10As = constraintAnnotation.treatCheck10As();
        this.treatCheck11As = constraintAnnotation.treatCheck11As();

        if (!Character.isLetterOrDigit(this.treatCheck10As)) {
            throw new IllegalArgumentException("'" + treatCheck10As + "' não é um dígito nem uma letra.");
        }

        if (!Character.isLetterOrDigit(this.treatCheck11As)) {
            throw new IllegalArgumentException("'" + this.treatCheck11As + "' não é um dígito nem uma letra.");
        }
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        return false;
    }

    /**
     * Validar o dígito de verificação usando a soma de verificação Mod11
     *
     * @param digits     Os dígitos sobre os quais calcular a soma de verificação
     * @param checkDigit o dígito de verificação
     * @return {@code true} se o resultado mod11 corresponder ao dígito de verificação, {@code false} caso contrário
     */
    @Override
    public boolean isCheckDigitValid(List<Integer> digits, char checkDigit) {
        if (reverseOrder) {
            Collections.reverse(digits);
        }

        int modResult = ModUtil.calculateMod11Check(digits, this.threshold);
        switch (modResult) {
            case 10:
                return checkDigit == this.treatCheck10As;
            case 11:
                return checkDigit == this.treatCheck11As;
            default:
                return Character.isDigit(checkDigit) && modResult == extractDigit(checkDigit);
        }
    }

}
