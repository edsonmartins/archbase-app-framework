package br.com.archbase.ddd.infraestructure.persistence.jpa.specification;

import br.com.archbase.ddd.domain.specification.ArchbaseSpecification;
import org.springframework.core.GenericTypeResolver;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementação padrão de {@link SpecificationTranslator} que registra e aplica conversores de especificação,
 * com base em uma estrutura de dados de mapa. Sempre que nenhum conversor foi encontrado para a especificação fornecida, olhamos
 * para um conversor na superclasse da especificação. Se, mesmo para as superclasses, nenhum conversor poderia ser
 * encontrado, somos forçados a lançar uma exceção de tempo de execução.
 *
 * @author edsonmartins
 */
@SuppressWarnings("all")
public class SpecificationTranslatorImpl implements SpecificationTranslator {
    private Map<Class<?>, SpecificationConverter<?, ?>> converterMapping = new HashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Predicate translateToPredicate(ArchbaseSpecification<T> archbaseSpecification, Root<T> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
        if (archbaseSpecification instanceof JpaArchbaseSpecification<?>) {
            // As especificações de predicado são capazes de resolver seus próprios predicados
            return ((JpaArchbaseSpecification<T>) archbaseSpecification).toPredicate(root, cq, cb);
        } else {
            // As especificações orientadas ao domínio requerem um conversor para resolver seu predicado
            SpecificationConverter<ArchbaseSpecification<T>, T> converter = findConverter(archbaseSpecification);
            // Sempre que nenhum conversor for encontrado, o predicado não pode ser resolvido
            if (converter == null) {
                String message = String.format("ArchbaseSpecification [%s] não tem conversores registrados.", archbaseSpecification.getClass().getName());
                throw new IllegalArgumentException(message);
            }
            // Delegar a conversão para nossa instância do conversor correspondente
            return converter.convertToPredicate(archbaseSpecification, root, cq, cb);
        }
    }

    /**
     * Recupere o conversor, capaz de traduzir nossa especificação fornecida. Sempre que nenhum conversor poderia
     * ser encontrado para a especificação fornecida, procuramos um conversor na superclasse da especificação.
     *
     * @param <T>           tipo de entidade que está sendo usada em nossa especificação
     * @param <S>           tipo de especificação sendo convertida
     * @param specification a especificação para a qual um conversor correspondente deve ser encontrado
     * @return um conversor correspondente, se houver
     */
    @SuppressWarnings("unchecked")
    private <T, S extends ArchbaseSpecification<T>> SpecificationConverter<S, T> findConverter(S specification) {
        Class<?> specificationClass = specification.getClass();
        Object converter = null;
        do {
            converter = converterMapping.get(specificationClass);
        } while (converter == null && (specificationClass = specificationClass.getSuperclass()) != null);
        return (SpecificationConverter<S, T>) converter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SpecificationTranslator registerConverter(SpecificationConverter<?, ?> converter) {
        Class<?> specificationClass = GenericTypeResolver.resolveTypeArguments(converter.getClass(), SpecificationConverter.class)[0];
        converterMapping.put(specificationClass, converter);
        return this; // Retorne esta instância para habilitar o encadeamento
    }

}
