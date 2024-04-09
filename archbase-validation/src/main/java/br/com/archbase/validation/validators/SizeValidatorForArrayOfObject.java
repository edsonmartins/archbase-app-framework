package br.com.archbase.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.Size;
import java.lang.reflect.Array;

/**
 * Verifique se o comprimento de uma matriz está entre <i> min </i> e <i> max </i>
 */
public class SizeValidatorForArrayOfObject extends SizeValidator
        implements ConstraintValidator<Size, Object[]> {
    /**
     * Verifica o número de entradas em uma matriz.
     *
     * @param array   O array a ser validado.
     * @param context de contexto no qual a restrição é avaliada.
     * @return Retorna <code> true </code> se a matriz for <code> null </code> ou o número de entradas em
     * <code> array </code> está entre os valores <code> min </code> e <code> max </code> especificados (inclusive),
     * <code> false </code> caso contrário.
     */
    public boolean isValid(Object[] array, ConstraintValidatorContext context) {
        if (array == null) {
            return true;
        }
        int length = Array.getLength(array);
        return length >= min && length <= max;
    }

}
