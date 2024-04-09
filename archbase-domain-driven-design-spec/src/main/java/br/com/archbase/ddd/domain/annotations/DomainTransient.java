package br.com.archbase.ddd.domain.annotations;

import java.lang.annotation.*;

/**
 * Identifica uma {@link DomainTransient}. Marca um campo de Entidade de dominio como transient(temporário).
 *
 * @author edsonmartins
 * @see <a href="https://domainlanguage.com/wp-content/uploads/2016/05/DDD_Reference_2015-03.pdf">Domain-Driven Design
 * Reference (Evans) - Entities</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface DomainTransient {

}
