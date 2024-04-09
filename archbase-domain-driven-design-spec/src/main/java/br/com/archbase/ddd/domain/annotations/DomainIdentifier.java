package br.com.archbase.ddd.domain.annotations;

import br.com.archbase.ddd.domain.contracts.Identifier;

import java.lang.annotation.*;

/**
 * Identifica uma {@link DomainIdentifier}. Identificadores são parte das Entidades de dominio. Para que elas
 * possam ser identificáveis precisam estar marcadas com a interface {@link Identifier}.
 * Usamos esta anotação documentações e geração de código.
 *
 * @author edsonmartins
 * @see <a href="https://domainlanguage.com/wp-content/uploads/2016/05/DDD_Reference_2015-03.pdf">Domain-Driven Design
 * Reference (Evans) - Entities</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface DomainIdentifier {

}
