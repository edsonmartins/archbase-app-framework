package br.com.archbase.hypersistence.repository;

import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementação dos métodos otimizados do repositório Hypersistence.
 * <p>
 * Esta classe fornece implementações eficientes dos métodos persist, merge e update
 * que evitam SELECTs desnecessários antes de operações de escrita.
 * </p>
 *
 * @param <T>  Tipo da entidade
 * @param <ID> Tipo do identificador da entidade
 * @author Archbase Team
 * @since 2.1.0
 */
@Transactional(readOnly = true)
public class ArchbaseHypersistenceRepositoryImpl<T, ID> extends SimpleJpaRepository<T, ID>
        implements ArchbaseHypersistenceRepository<T, ID> {

    private final EntityManager entityManager;

    public ArchbaseHypersistenceRepositoryImpl(JpaEntityInformation<T, ?> entityInformation,
                                                EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
    }

    public ArchbaseHypersistenceRepositoryImpl(Class<T> domainClass, EntityManager entityManager) {
        super(domainClass, entityManager);
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public <S extends T> S persist(S entity) {
        entityManager.persist(entity);
        return entity;
    }

    @Override
    @Transactional
    public <S extends T> S persistAndFlush(S entity) {
        S result = persist(entity);
        entityManager.flush();
        return result;
    }

    @Override
    @Transactional
    public <S extends T> List<S> persistAll(Iterable<S> entities) {
        List<S> result = new ArrayList<>();
        for (S entity : entities) {
            result.add(persist(entity));
        }
        return result;
    }

    @Override
    @Transactional
    public <S extends T> List<S> persistAllAndFlush(Iterable<S> entities) {
        List<S> result = persistAll(entities);
        entityManager.flush();
        return result;
    }

    @Override
    @Transactional
    public <S extends T> S merge(S entity) {
        return entityManager.merge(entity);
    }

    @Override
    @Transactional
    public <S extends T> S mergeAndFlush(S entity) {
        S result = merge(entity);
        entityManager.flush();
        return result;
    }

    @Override
    @Transactional
    public <S extends T> List<S> mergeAll(Iterable<S> entities) {
        List<S> result = new ArrayList<>();
        for (S entity : entities) {
            result.add(merge(entity));
        }
        return result;
    }

    @Override
    @Transactional
    public <S extends T> List<S> mergeAllAndFlush(Iterable<S> entities) {
        List<S> result = mergeAll(entities);
        entityManager.flush();
        return result;
    }

    @Override
    @Transactional
    public <S extends T> S update(S entity) {
        return entityManager.merge(entity);
    }

    @Override
    @Transactional
    public <S extends T> S updateAndFlush(S entity) {
        S result = update(entity);
        entityManager.flush();
        return result;
    }

    @Override
    @Transactional
    public <S extends T> List<S> updateAll(Iterable<S> entities) {
        List<S> result = new ArrayList<>();
        for (S entity : entities) {
            result.add(update(entity));
        }
        return result;
    }

    @Override
    @Transactional
    public <S extends T> List<S> updateAllAndFlush(Iterable<S> entities) {
        List<S> result = updateAll(entities);
        entityManager.flush();
        return result;
    }
}
