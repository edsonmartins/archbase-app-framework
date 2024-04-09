package br.com.archbase.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.Size;
import java.util.Collection;

/**
 * Verifique se o tamanho da coleção está entre o mínimo e o máximo.
 */
public class SizeValidatorForCollection extends SizeValidator
        implements ConstraintValidator<Size, Collection<?>> {

    /**
     * Verifica o número de entradas em um mapa.
     *
     * @param collection A coleção a ser validada.
     * @param context    de contexto no qual a restrição é avaliada.
     * @return Retorna <code> true </code> se a coleção for <code> null </code> ou o número de entradas em
     * <code> collection </code> está entre os valores <code> min </code> e <code> max </code> especificados (inclusive),
     * <code> false </code> caso contrário.
     */
    public boolean isValid(Collection<?> collection, ConstraintValidatorContext context) {
        if (collection == null) {
            return true;
        }
        int length = collection.size();
        return length >= min && length <= max;
    }

}
