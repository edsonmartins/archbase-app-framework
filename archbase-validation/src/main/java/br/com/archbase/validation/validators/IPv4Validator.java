package br.com.archbase.validation.validators;

import br.com.archbase.shared.kernel.utils.InetAddressValidator;
import br.com.archbase.validation.constraints.IPv4;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class IPv4Validator implements ConstraintValidator<IPv4, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        return InetAddressValidator
                .getInstance()
                .isValidInet4Address(value);
    }

    @Override
    public void initialize(IPv4 constraintAnnotation) {
        //
    }
}
