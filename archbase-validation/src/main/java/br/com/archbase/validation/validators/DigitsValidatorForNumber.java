package br.com.archbase.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

/**
 * Valida se o <code>Número</code> sendo validado corresponde ao padrão
 * definido na restrição.
 */
public class DigitsValidatorForNumber implements ConstraintValidator<Digits, Number> {

    private int integral;
    private int fractional;

    public int getIntegral() {
        return integral;
    }

    public void setIntegral(int integral) {
        this.integral = integral;
    }

    public int getFractional() {
        return fractional;
    }

    public void setFractional(int fractional) {
        this.fractional = fractional;
    }

    @Override
    public void initialize(Digits annotation) {
        this.integral = annotation.integer();
        this.fractional = annotation.fraction();
        if (integral < 0) {
            throw new IllegalArgumentException(
                    "O comprimento da parte inteira não pode ser negativo.");
        }
        if (fractional < 0) {
            throw new IllegalArgumentException(
                    "O comprimento da parte fracionária não pode ser negativo.");
        }
    }

    public boolean isValid(Number num, ConstraintValidatorContext context) {
        if (num == null) {
            return true;
        }

        BigDecimal bigDecimal;
        if (num instanceof BigDecimal) {
            bigDecimal = (BigDecimal) num;
        } else {
            bigDecimal = new BigDecimal(num.toString());
        }
        bigDecimal = bigDecimal.stripTrailingZeros();

        int intLength = bigDecimal.precision() - bigDecimal.scale();
        if (integral >= intLength) {
            int factionLength = bigDecimal.scale() < 0 ? 0 : bigDecimal.scale();
            return fractional >= factionLength;
        } else {
            return false;
        }
    }
}
