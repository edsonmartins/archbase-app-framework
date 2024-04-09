package br.com.archbase.ddd.domain.contracts;


import java.util.Optional;

public interface CreateOrUpdateOrRemoveEntityUseCase<T,R> {
    R createEntity(T entity) ;
    R updateEntity(T entity) ;
    Optional<R> getEntityById(String id) ;
    R removeEntity(String id) ;
}

