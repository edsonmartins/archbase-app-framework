package br.com.archbase.security.repository;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import br.com.archbase.security.persistence.ApiTokenEntity;

/**
 * Repository para ApiTokenEntity.
 *
 * Os métodos de busca por token foram movidos para ApiTokenPersistenceAdapter
 * para usar QueryDSL e evitar problemas com filtro automático de tenant.
 *
 * @see br.com.archbase.security.adapter.ApiTokenPersistenceAdapter#findByToken(String)
 * @see br.com.archbase.security.adapter.ApiTokenPersistenceAdapter#findByTokenAndTenantId(String, String)
 */
public interface ApiTokenRepository extends ArchbaseCommonJpaRepository<ApiTokenEntity, String, Long> {
}
