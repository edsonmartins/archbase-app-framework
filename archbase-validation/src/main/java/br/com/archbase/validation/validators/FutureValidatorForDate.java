package br.com.archbase.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.Future;
import java.util.Date;

/**
 * Valida uma data ou calendário que representa uma data no futuro <br/>
 */
public class FutureValidatorForDate implements ConstraintValidator<Future, Date> {

    @Override
    public void initialize(Future annotation) {
        //
    }

    public boolean isValid(Date date, ConstraintValidatorContext context) {
        return date == null || date.after(now());
    }

    /**
     * sobrescrever quando você precisa de um algoritmo diferente para 'now'.
     *
     * @return data / hora atual
     */
    protected Date now() {
        return new Date();
    }
}
