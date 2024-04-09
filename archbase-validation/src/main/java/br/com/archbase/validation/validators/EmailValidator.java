package br.com.archbase.validation.validators;

import br.com.archbase.shared.kernel.utils.EMailValidationUtils;
import br.com.archbase.validation.constraints.Email;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;


/**
 * Verifica se um determinado endereço de e-mail é válido.
 */
public class EmailValidator implements ConstraintValidator<Email, CharSequence> {

    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        return EMailValidationUtils.isValid(value);
    }

    @Override
    public void initialize(Email parameters) {
        // não fazer nada (desde que Email não tenha propriedades)
    }
}
