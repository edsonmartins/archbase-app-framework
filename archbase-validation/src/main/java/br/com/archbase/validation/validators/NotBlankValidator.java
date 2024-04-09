package br.com.archbase.validation.validators;

import br.com.archbase.validation.constraints.NotBlank;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Verifique se o comprimento aparado de uma sequência de caracteres (por exemplo, string) não está vazio.
 */
public class NotBlankValidator implements ConstraintValidator<NotBlank, CharSequence> {

    @Override
    public void initialize(NotBlank annotation) {
        //
    }

    /**
     * Verifica se a string aparada não está vazia.
     *
     * @param charSequence               A sequência de caracteres a ser validada.
     * @param constraintValidatorContext contexto no qual a restrição é avaliada.
     * @return Retorna <code> true </code> se a string é <code> null </code> ou o comprimento de <code> charSequence </code> entre os especificados
     * Valores <code> min </code> e <code> max </code> (inclusive), <code> false </code> caso contrário.
     */
    public boolean isValid(CharSequence charSequence, ConstraintValidatorContext constraintValidatorContext) {
        if (charSequence == null) {
            return true;
        }

        return charSequence.toString().trim().length() > 0;
    }
}
