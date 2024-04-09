package br.com.archbase.ddd.domain.specification;

/**
 * In computer programming, the specification pattern is a particular software design
 * pattern, whereby business logic can be recombined by chaining the business logic
 * together using boolean logic.
 *
 * @param <T> type of candidates being checked
 * @author edsonmartins
 */
public interface ArchbaseSpecification<T> {

    /**
     * See if an object satisfies all the requirements expressed in this specification.
     *
     * @param candidate the object being verified
     * @return {@code true} if the requirements are satisfied, otherwise {@code false}
     */
    boolean isSatisfiedBy(T candidate);

}
