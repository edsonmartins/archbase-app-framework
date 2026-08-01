package br.com.archbase.security.repository;


import br.com.archbase.security.persistence.ApiTokenEntity;
import br.com.archbase.security.util.ApiTokenHasher;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ApiTokenNativeRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Busca pelo hash do token; o valor em claro só é comparado em linha ainda não migrada
     * ({@code token_hash IS NULL}). Ver {@code ApiTokenPersistenceAdapter} para o racional.
     */
    public Optional<ApiTokenEntity> findByTokenAndTenantId(String token, String tenantId) {
        String sql = "SELECT * FROM seguranca_token_api "
                + "WHERE (token_hash = :tokenHash OR (token_hash IS NULL AND token = :token)) "
                + "AND tenant_id = :tenantId";
        Query query = entityManager.createNativeQuery(sql, ApiTokenEntity.class);
        query.setParameter("tokenHash", ApiTokenHasher.hash(token));
        query.setParameter("token", token);
        query.setParameter("tenantId", tenantId);
        return query.getResultList().stream().findFirst();
    }
}