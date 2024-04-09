package br.com.archbase.ddd.domain.annotations;

import java.lang.annotation.*;

/**
 * Identifica uma {@link DomainEntity}. Entidades representam um fio de continuidade e identidade, passando por um ciclo de vida,
 * embora seus atributos possam mudar. Os meios de identificação podem vir de fora ou podem ser arbitrários
 * identificador criado por e para o sistema, mas deve corresponder às distinções de identidade no modelo. O modelo
 * deve definir o que significa ser a mesma coisa.
 *
 * @see <a href="https://domainlanguage.com/wp-content/uploads/2016/05/DDD_Reference_2015-03.pdf">Domain-Driven Design
 * Reference (Evans) - Entities</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface DomainEntity {

}
