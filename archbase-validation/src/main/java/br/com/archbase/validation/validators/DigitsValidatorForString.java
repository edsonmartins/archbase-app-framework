package br.com.archbase.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

/**
 * Valida que a <code>String</code> sendo validada consiste em dígitos * e corresponde
 * ao padrão definido na restrição.
 */
public class DigitsValidatorForString implements ConstraintValidator<Digits, String> {

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

    public boolean isValid(String str, ConstraintValidatorContext context) {
        if (str == null) {
            return true;
        }
        BigDecimal bigDecimal = getBigDecimalValue(str);
        if (bigDecimal == null) {
            return false;
        }
        int intLength = bigDecimal.precision() - bigDecimal.scale();
        if (integral >= intLength) {
            int factionLength = bigDecimal.scale() < 0 ? 0 : bigDecimal.scale();
            return fractional >= factionLength;
        } else {
            return false;
        }
    }

    private BigDecimal getBigDecimalValue(String str) {
        BigDecimal bd;
        try {
            bd = new BigDecimal(str);
        } catch (NumberFormatException nfe) {
            return null;
        }
        return bd;
    }
}
