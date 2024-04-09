package br.com.archbase.validation.validators;

import br.com.archbase.validation.constraints.NIT;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Valida a cadeia gerada através do método {@linkplain #toString()} para
 * verificar se ela está de acordo com o padrão de um NIT. O padrão NIT é o
 * mesmo utilizado no PIS, PASEP e o SUS.
 */
public class NITValidator implements ConstraintValidator<NIT, String> {
    private br.com.caelum.stella.validation.NITValidator stellaValidator;

    @Override
    public void initialize(NIT nit) {
        AnnotationMessageProducer messageProducer = new AnnotationMessageProducer(
                nit);
        stellaValidator = new br.com.caelum.stella.validation.NITValidator(messageProducer, nit.formatted());
    }

    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value != null) {
            if (value.trim().length() == 0) {
                return true;
            } else {
                return stellaValidator.invalidMessagesFor(value).isEmpty();
            }
        } else {
            return true;
        }
    }

}
