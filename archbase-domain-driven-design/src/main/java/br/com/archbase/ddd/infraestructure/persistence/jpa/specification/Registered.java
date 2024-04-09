package br.com.archbase.ddd.infraestructure.persistence.jpa.specification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Indica que um conversor de especificação será registrado automaticamente.
 *
 * @author edsonmartins
 */
@Target(value = {ElementType.TYPE})
public @interface Registered {
    // Nenhuma lógica interna necessária, apenas usado para anexar metadados aos nossos conversores
}
