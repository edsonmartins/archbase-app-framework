package br.com.archbase.ddd.domain.specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe utilitária para especificações.
 *
 * @author edsonmartins
 */
public final class Specifications {

    // Oculto para evitar a inicialização
    private Specifications() {
        super();
    }

    /**
     * Satisfeito por todos ?
     *
     * @param <T>                   Tipo de classe
     * @param archbaseSpecification especificação
     * @param candidates            candidatos
     * @return True se satifeito por todos
     */
    public static <T> boolean isSatisfiedByAll(ArchbaseSpecification<T> archbaseSpecification, Iterable<T> candidates) {
        for (T candidate : candidates) {
            if (!archbaseSpecification.isSatisfiedBy(candidate)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Satisfeito por alguns ?
     *
     * @param <T>                   Tipo de classe
     * @param archbaseSpecification especificação
     * @param candidates            candidatos
     * @return True se satisfeito por alguns
     */
    public static <T> boolean isSatisfiedBySome(ArchbaseSpecification<T> archbaseSpecification, Iterable<T> candidates) {
        for (T candidate : candidates) {
            if (archbaseSpecification.isSatisfiedBy(candidate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Recupera todos que satisfazem a especifiação.
     *
     * @param <T>                   Tipo de classe
     * @param archbaseSpecification especificação
     * @param candidates            candidatos
     * @return Lista de objetos que satisfazem a especificação
     */
    public static <T> List<T> findAllSatisfying(ArchbaseSpecification<T> archbaseSpecification, Iterable<T> candidates) {
        List<T> selection = new ArrayList<>();
        for (T candidate : candidates) {
            if (archbaseSpecification.isSatisfiedBy(candidate)) {
                selection.add(candidate);
            }
        }
        return selection;
    }

    /**
     * Retorna quantidade que satisfazem a especificação.
     *
     * @param <T>                   Tipo de classe
     * @param archbaseSpecification especificação
     * @param candidates            candidatos
     * @return quantidade que satisfazem a especificação
     */
    public static <T> long countAllSatisfying(ArchbaseSpecification<T> archbaseSpecification, Iterable<T> candidates) {
        long numberOfMatches = 0;
        for (T candidate : candidates) {
            if (archbaseSpecification.isSatisfiedBy(candidate)) {
                numberOfMatches++;
            }
        }
        return numberOfMatches;
    }

    /**
     * NOT
     *
     * @param archbaseSpecification especificação
     * @return
     */
    public static <T> ArchbaseSpecification<T> not(ArchbaseSpecification<T> archbaseSpecification) {
        return new NotArchbaseSpecification<>(archbaseSpecification);
    }

    /**
     * AND
     *
     * @param <T> Tipo de classe
     * @param lhs especificação da direita
     * @param rhs especificação da esquerda
     * @return
     */
    public static <T> ArchbaseSpecification<T> and(ArchbaseSpecification<T> lhs, ArchbaseSpecification<T> rhs) {
        return new AndArchbaseSpecification<>(lhs, rhs);
    }

    /**
     * OR
     *
     * @param <T> Tipo de classe
     * @param lhs especificação da direita
     * @param rhs especificação da esquerda
     * @return
     */
    public static <T> ArchbaseSpecification<T> or(ArchbaseSpecification<T> lhs, ArchbaseSpecification<T> rhs) {
        return new OrArchbaseSpecification<>(lhs, rhs);
    }

}
