package br.com.archbase.validation.validators;

import br.com.archbase.validation.constraints.CNPJ;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Valida a cadeia gerada através do método {@linkplain #toString()} para
 * verificar se ela está de acordo com o padrão de CNPJ.
 */
public class CNPJValidator implements ConstraintValidator<CNPJ, String> {
    private br.com.caelum.stella.validation.CNPJValidator stellaValidator;

    @Override
    public void initialize(CNPJ cnpj) {
        AnnotationMessageProducer messageProducer = new AnnotationMessageProducer(cnpj);
        stellaValidator = new br.com.caelum.stella.validation.CNPJValidator(messageProducer, cnpj.formatted());
    }

    public boolean isValid(String cnpj, ConstraintValidatorContext context) {
        if (cnpj != null) {
            if (cnpj.trim().length() == 0) {
                return true;
            } else {
                return stellaValidator.invalidMessagesFor(cnpj).isEmpty();
            }
        } else {
            return true;
        }
    }
}
