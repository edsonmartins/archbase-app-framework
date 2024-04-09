package br.com.archbase.validation.validators;

import br.com.archbase.validation.constraints.CPF;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Valida a cadeia gerada através do método {@linkplain #toString()} para
 * verificar se ela está de acordo com o padrão de CPF.
 */
public class CPFValidator implements ConstraintValidator<CPF, String> {
    private br.com.caelum.stella.validation.CPFValidator stellaValidator;


    @Override
    public void initialize(CPF cpf) {
        AnnotationMessageProducer messageProducer = new AnnotationMessageProducer(cpf);
        stellaValidator = new br.com.caelum.stella.validation.CPFValidator(messageProducer, cpf.formatted(), cpf.ignoreRepeated());
    }

    public boolean isValid(String cpf, ConstraintValidatorContext context) {
        if (cpf != null) {
            if (cpf.trim().length() == 0) {
                return true;
            } else {
                return stellaValidator.invalidMessagesFor(cpf).isEmpty();
            }
        } else {
            return true;
        }
    }
}
