package br.com.archbase.ddd.domain.contracts;

public interface EntityPersistenceMapper<D,P> {

    P toEntity(D entity);

    D toDomain(P entity);

}