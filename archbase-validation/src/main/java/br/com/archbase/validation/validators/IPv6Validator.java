package br.com.archbase.validation.validators;

import br.com.archbase.shared.kernel.utils.InetAddressValidator;
import br.com.archbase.validation.constraints.IPv6;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class IPv6Validator implements ConstraintValidator<IPv6, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        return InetAddressValidator
                .getInstance()
                .isValidInet6Address(value);
    }

    @Override
    public void initialize(IPv6 constraintAnnotation) {
        //
    }
}
