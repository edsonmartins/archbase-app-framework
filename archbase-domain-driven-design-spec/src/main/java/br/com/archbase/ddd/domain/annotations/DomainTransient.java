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
// FIELD e METHOD: o processador de anotações do Archbase usa esta anotação para ignorar a propriedade (papel de @Transient), que vale em campo e getter.
// Até a 3.5.0 só TYPE era permitido, e não dava para colocá-la onde ela tem efeito.
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
@Documented
public @interface DomainTransient {

}
