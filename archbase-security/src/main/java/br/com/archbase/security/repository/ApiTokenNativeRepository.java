package br.com.archbase.security.repository;


import br.com.archbase.security.persistence.ApiTokenEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ApiTokenNativeRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<ApiTokenEntity> findByTokenAndTenantId(String token, String tenantId) {
        String sql = "SELECT * FROM seguranca_token_api WHERE token = :token AND tenant_id = :tenantId";
        Query query = entityManager.createNativeQuery(sql, ApiTokenEntity.class);
        query.setParameter("token", token);
        query.setParameter("tenantId", tenantId);
        return query.getResultList().stream().findFirst();
    }
}