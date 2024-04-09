package br.com.archbase.ddd.domain.annotations;

import java.lang.annotation.*;

/**
 * Identifica um objeto de valor. Os conceitos de domínio que são modelados como objetos de valor não têm identidade conceitual ou
 * ciclo da vida. As implementações devem ser imutáveis, as operações não têm efeitos colaterais.
 *
 * @see <a href="https://domainlanguage.com/wp-content/uploads/2016/05/DDD_Reference_2015-03.pdf">Domain-Driven Design
 * Reference (Evans) - Value objects</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface DomainValueObject {

}
