package br.com.archbase.validation.validators;

import br.com.archbase.shared.kernel.utils.ModUtil;
import br.com.archbase.validation.constraints.Mod10Check;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;

/**
 * Mod10 (implementação do algoritmo de Luhn) Verifique o validador
 * <p>
 * http://en.wikipedia.org/wiki/Luhn_algorithm
 * http://en.wikipedia.org/wiki/Check_digit
 */
public class Mod10CheckValidator extends ModCheckBase
        implements ConstraintValidator<Mod10Check, CharSequence> {

    /**
     * Multiplicador a ser usado por dígitos ímpares no algoritmo Mod10
     */
    private int multiplier;

    /**
     * Peso a ser usado por dígitos pares no algoritmo Mod10
     */
    private int weight;


    @Override
    public void initialize(Mod10Check constraintAnnotation) {
        super.initialize(
                constraintAnnotation.startIndex(),
                constraintAnnotation.endIndex(),
                constraintAnnotation.checkDigitIndex(),
                constraintAnnotation.ignoreNonDigitCharacters()
        );
        this.multiplier = constraintAnnotation.multiplier();
        this.weight = constraintAnnotation.weight();

        if (this.multiplier < 0) {
            throw new IllegalArgumentException("Multiplicador não pode ser negativo: " + multiplier);
        }
        if (this.weight < 0) {
            throw new IllegalArgumentException("Peso não pode ser negativo: " + weight);
        }
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        return false;
    }

    /**
     * Valide o dígito de verificação usando Mod10
     *
     * @param digits     Os dígitos sobre os quais calcular a soma de verificação
     * @param checkDigit o dígito de verificação
     * @return {@code true} se o resultado mod 10 corresponder ao dígito de verificação, {@code false} caso contrário
     */
    @Override
    public boolean isCheckDigitValid(List<Integer> digits, char checkDigit) {
        int modResult = ModUtil.calculateMod10Check(digits, this.multiplier, this.weight);

        if (!Character.isDigit(checkDigit)) {
            return false;
        }

        int checkValue = extractDigit(checkDigit);
        return checkValue == modResult;
    }
}
