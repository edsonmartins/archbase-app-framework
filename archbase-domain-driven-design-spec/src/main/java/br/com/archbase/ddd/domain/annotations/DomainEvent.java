package br.com.archbase.ddd.domain.annotations;


import java.lang.annotation.*;

/**
 * Identifica uma {@link DomainEvent}. Um evento é algo que ocorreu no passado. Um evento de domínio é algo
 * que ocorreu no domínio que você deseja que outras partes do mesmo domínio (em processo) tenham conhecimento.
 * As partes notificadas geralmente reagem de alguma forma aos eventos.
 * Um benefício importante dos eventos de domínio é que os efeitos colaterais podem ser expressos explicitamente.
 *
 * @see <a href="https://domainlanguage.com/wp-content/uploads/2016/05/DDD_Reference_2015-03.pdf">Domain-Driven Design
 * Reference (Evans) - Entities</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface DomainEvent {
}
