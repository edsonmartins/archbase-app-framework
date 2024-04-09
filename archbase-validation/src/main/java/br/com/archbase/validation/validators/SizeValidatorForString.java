package br.com.archbase.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.Size;

/**
 * Verifique se o comprimento da string está entre mínimo e máximo.
 */
public class SizeValidatorForString extends SizeValidator
        implements ConstraintValidator<Size, String> {
    /**
     * Verifica o comprimento da string especificada.
     *
     * @param s       A string a ser validada.
     * @param context de contexto no qual a restrição é avaliada.
     * @return Retorna <code> true </code> se a string é <code> null </code> ou o comprimento de <code> s </code> entre os especificados
     * <code> min </code> e <code> max </code> valores (inclusive), <code> false </code> caso contrário.
     */
    public boolean isValid(String s, ConstraintValidatorContext context) {
        if (s == null) {
            return true;
        }
        int length = s.length();
        return length >= min && length <= max;
    }

}
