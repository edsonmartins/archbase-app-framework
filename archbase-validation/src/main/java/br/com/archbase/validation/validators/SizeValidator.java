package br.com.archbase.validation.validators;

import jakarta.validation.ValidationException;
import jakarta.validation.constraints.Size;

/**
 * Implementação do validador abstrato para anotação @Size <br/>
 */
public abstract class SizeValidator {
    protected int min;
    protected int max;

    /**
     * Configure o validador de restrição com base nos elementos
     * especificado no momento em que foi definido.
     *
     * @param constraint a definição de restrição
     */
    public void initialize(Size constraint) {
        min = constraint.min();
        max = constraint.max();
        if (min < 0) throw new ValidationException("Min não pode ser negativo");
        if (max < 0) throw new ValidationException("Max não pode ser negativo");
        if (max < min) throw new ValidationException("Max não pode ser menor que Min");
    }
}