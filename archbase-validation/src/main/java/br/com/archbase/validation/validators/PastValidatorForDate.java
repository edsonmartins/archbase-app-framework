package br.com.archbase.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.Past;
import java.util.Date;

/**
 * Valide uma data ou calendário que representa uma data no passado <br/>
 */
public class PastValidatorForDate implements ConstraintValidator<Past, Date> {

    @Override
    public void initialize(Past annotation) {
        //
    }

    public boolean isValid(Date date, ConstraintValidatorContext context) {
        return date == null || date.before(now());
    }

    /**
     * Sobrescrever quando você precisa de um algoritmo diferente para 'now'.
     *
     * @return data / hora atual
     */
    protected Date now() {
        return new Date();
    }
}
