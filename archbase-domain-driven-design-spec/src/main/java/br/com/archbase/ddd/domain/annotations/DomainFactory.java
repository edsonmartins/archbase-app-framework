package br.com.archbase.ddd.domain.annotations;

import java.lang.annotation.*;

/**
 * Identifica uma {@link DomainFactory}. As fábricas encapsulam a responsabilidade de criar objetos complexos em geral e
 * Agregados em particular. Os objetos retornados pelos métodos de fábrica têm a garantia de estar em estado válido.
 *
 * @see DomainAggregateRoot
 * @see <a href="https://domainlanguage.com/wp-content/uploads/2016/05/DDD_Reference_2015-03.pdf">Domain-Driven Design
 * Reference (Evans) - Factories</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface DomainFactory {

}
