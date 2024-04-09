package br.com.archbase.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.Past;
import java.util.Calendar;

/**
 * Valida uma data ou calendário que representa uma data no passado <br/>
 */
public class PastValidatorForCalendar implements ConstraintValidator<Past, Calendar> {

    @Override
    public void initialize(Past annotation) {
        //
    }

    public boolean isValid(Calendar cal, ConstraintValidatorContext context) {
        return cal == null || cal.before(now());
    }


    /**
     * sobrescrever quando precisar de um algoritmo diferente para 'now'.
     *
     * @return data / hora atual
     */
    protected Calendar now() {
        return Calendar.getInstance();
    }
}
