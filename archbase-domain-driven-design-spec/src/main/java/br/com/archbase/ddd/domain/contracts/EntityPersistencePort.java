package br.com.archbase.ddd.domain.contracts;


import java.util.Optional;

public interface EntityPersistencePort<T,R> {

    R saveEntity(T entity) ;

    R removeEntity(T entity) ;

    Optional<R> getEntityById(String id) ;

    Optional<R> getEntityByName(String name) ;

    boolean existsEntityByName(String name) ;
}
