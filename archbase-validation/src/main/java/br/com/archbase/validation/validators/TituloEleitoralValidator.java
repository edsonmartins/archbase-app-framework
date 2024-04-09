package br.com.archbase.validation.validators;

import br.com.archbase.validation.constraints.TituloEleitoral;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Valida a cadeia gerada através do método {@linkplain #toString()} para
 * verificar se ela está de acordo com o padrão de Título Eleitoral.
 */
public class TituloEleitoralValidator implements
        ConstraintValidator<TituloEleitoral, String> {
    private br.com.caelum.stella.validation.TituloEleitoralValidator stellaValidator;

    @Override
    public void initialize(TituloEleitoral tituloEleitoral) {
        AnnotationMessageProducer messageProducer = new AnnotationMessageProducer(
                tituloEleitoral);
        stellaValidator = new br.com.caelum.stella.validation.TituloEleitoralValidator(messageProducer);
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
