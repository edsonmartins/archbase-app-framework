package br.com.archbase.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.Future;
import java.util.Calendar;

/**
 * Valida uma data ou calendário que representa uma data no futuro <br/>
 */
public class FutureValidatorForCalendar implements ConstraintValidator<Future, Calendar> {

    @Override
    public void initialize(Future annotation) {
        //
    }

    public boolean isValid(Calendar cal, ConstraintValidatorContext context) {
        return cal == null || cal.after(now());
    }


    /**
     * substituir quando precisar de um algoritmo diferente para 'now'.
     *
     * @return data / hora atual
     */
    protected Calendar now() {
        return Calendar.getInstance();
    }
}
