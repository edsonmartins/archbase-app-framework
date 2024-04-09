package br.com.archbase.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.Size;
import java.util.Map;

/**
 * Verifique se o tamanho do mapa está entre mínimo e máximo.
 */
public class SizeValidatorForMap extends SizeValidator
        implements ConstraintValidator<Size, Map<?, ?>> {
    /**
     * Verifica o número de entradas em um mapa.
     *
     * @param map     O mapa a ser validado.
     * @param context de contexto no qual a restrição é avaliada.
     * @return Retorna <code> true </code> se o mapa for <code> null </code> ou o número de entradas em <code> map </code>
     * está entre os valores <code> min </code> e <code> max </code> especificados (inclusive),
     * <code> false </code> caso contrário.
     */
    public boolean isValid(Map<?, ?> map, ConstraintValidatorContext context) {
        if (map == null) {
            return true;
        }
        int size = map.size();
        return size >= min && size <= max;
    }

}
