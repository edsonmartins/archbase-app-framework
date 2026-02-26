package br.com.archbase.security.adapter;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.ddd.domain.contracts.FindDataWithFilterQuery;
import br.com.archbase.query.rsql.jpa.SortUtils;
import br.com.archbase.security.domain.dto.ApiTokenDto;
import br.com.archbase.security.persistence.ApiTokenEntity;
import br.com.archbase.security.repository.ApiTokenRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class ApiTokenPersistenceAdapter implements FindDataWithFilterQuery<String, ApiTokenDto> {

    private final ApiTokenRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    public ApiTokenPersistenceAdapter(ApiTokenRepository repository) {
        this.repository = repository;
    }

    /**
     * Busca um token API pelo valor do token usando query nativa.
     * Usa o tenant do contexto atual (ArchbaseTenantContext).
     *
     * @param token o valor do token
     * @return Optional contendo o ApiTokenEntity se encontrado
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public Optional<ApiTokenEntity> findByToken(String token) {
        String tenantId = ArchbaseTenantContext.getTenantId();
        log.debug("findByToken - Token={}, TenantId do contexto={}", token, tenantId);

        String sql = "SELECT * FROM seguranca_token_api WHERE token = :token AND tenant_id = :tenantId";

        List<ApiTokenEntity> results = entityManager.createNativeQuery(sql, ApiTokenEntity.class)
                .setParameter("token", token)
                .setParameter("tenantId", tenantId)
                .getResultList();

        if (!results.isEmpty()) {
            ApiTokenEntity result = results.get(0);
            log.debug("Token encontrado: ID={}, Nome={}, TenantId={}, Revoked={}, Activated={}, Expiration={}",
                    result.getId(), result.getName(), result.getTenantId(), result.getRevoked(),
                    result.getActivated(), result.getExpirationDate());
            return Optional.of(result);
        } else {
            log.debug("Token não encontrado para token={} e tenantId={}", token, tenantId);
            return Optional.empty();
        }
    }

    /**
     * Busca um token API pelo valor do token e tenant ID usando query nativa.
     *
     * @param token o valor do token
     * @param tenantId o ID do tenant
     * @return Optional contendo o ApiTokenEntity se encontrado
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public Optional<ApiTokenEntity> findByTokenAndTenantId(String token, String tenantId) {
        log.debug("findByTokenAndTenantId - Token={}, TenantId={}", token, tenantId);

        String sql = "SELECT * FROM seguranca_token_api WHERE token = :token AND tenant_id = :tenantId";

        List<ApiTokenEntity> results = entityManager.createNativeQuery(sql, ApiTokenEntity.class)
                .setParameter("token", token)
                .setParameter("tenantId", tenantId)
                .getResultList();

        if (!results.isEmpty()) {
            ApiTokenEntity result = results.get(0);
            log.debug("Token encontrado para tenant {}: ID={}, Nome={}, Revoked={}, Activated={}, Expiration={}",
                    tenantId, result.getId(), result.getName(), result.getRevoked(),
                    result.getActivated(), result.getExpirationDate());
            return Optional.of(result);
        } else {
            log.debug("Token não encontrado para tenant {}", tenantId);
            return Optional.empty();
        }
    }

    /**
     * Valida um token API usando query nativa.
     * Usa o tenant do contexto atual (ArchbaseTenantContext).
     * Verifica se o token existe, não está revogado, está ativado e não expirou.
     *
     * @param token o valor do token
     * @return true se o token é válido
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public boolean validateToken(String token) {
        String tenantId = ArchbaseTenantContext.getTenantId();
        LocalDateTime now = LocalDateTime.now();

        log.debug("validateToken - Token={}, TenantId do contexto={}, Now={}", token, tenantId, now);

        String sql = "SELECT * FROM seguranca_token_api WHERE token = :token AND tenant_id = :tenantId " +
                     "AND bo_revogado = 'N' AND bo_ativado = 'S' AND dh_expiracao > :now";

        List<ApiTokenEntity> results = entityManager.createNativeQuery(sql, ApiTokenEntity.class)
                .setParameter("token", token)
                .setParameter("tenantId", tenantId)
                .setParameter("now", now)
                .getResultList();

        boolean isValid = !results.isEmpty();

        if (isValid) {
            ApiTokenEntity result = results.get(0);
            log.info("Token válido: ID={}, Nome={}, TenantId={}, Expiration={}",
                    result.getId(), result.getName(), result.getTenantId(), result.getExpirationDate());
        } else {
            // Buscar para diagnóstico (sem filtros de validação)
            String diagSql = "SELECT * FROM seguranca_token_api WHERE token = :token AND tenant_id = :tenantId";
            List<ApiTokenEntity> diagResults = entityManager.createNativeQuery(diagSql, ApiTokenEntity.class)
                    .setParameter("token", token)
                    .setParameter("tenantId", tenantId)
                    .getResultList();

            if (!diagResults.isEmpty()) {
                ApiTokenEntity t = diagResults.get(0);
                log.warn("Token inválido - Revoked={}, Activated={}, Expired={} (expiration={}, now={})",
                        t.getRevoked(), t.getActivated(),
                        t.getExpirationDate() != null && t.getExpirationDate().isBefore(now),
                        t.getExpirationDate(), now);
            } else {
                log.warn("Token não encontrado no banco de dados para token={} e tenantId={}", token, tenantId);
            }
        }

        return isValid;
    }


    @Override
    public ApiTokenDto findById(String id) {
        Optional<ApiTokenEntity> byId = repository.findById(id);
        return byId.map(ApiTokenEntity::toDto).orElse(null);
    }

    @Override
    public Page<ApiTokenDto> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ApiTokenEntity> result = repository.findAll(pageable);
        List<ApiTokenDto> list = result.stream().map(ApiTokenEntity::toDto).toList();
        return new ApiTokenPersistenceAdapter.PageApiToken(list, pageable, result.getTotalElements());
    }

    @Override
    public Page<ApiTokenDto> findAll(int page, int size, String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        Page<ApiTokenEntity> result = repository.findAll(pageable);
        List<ApiTokenDto> list = result.stream().map(ApiTokenEntity::toDto).toList();
        return new ApiTokenPersistenceAdapter.PageApiToken(list, pageable, result.getTotalElements());
    }

    @Override
    public List<ApiTokenDto> findAll(List<String> ids) {
        List<ApiTokenEntity> result = repository.findAllById(ids);
        return result.stream().map(ApiTokenEntity::toDto).toList();
    }

    @Override
    public Page<ApiTokenDto> findWithFilter(String filter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ApiTokenEntity> result = repository.findAll(filter, pageable);
        List<ApiTokenDto> list = result.stream().map(ApiTokenEntity::toDto).toList();
        return new ApiTokenPersistenceAdapter.PageApiToken(list, pageable, result.getTotalElements());
    }

    @Override
    public Page<ApiTokenDto> findWithFilter(String filter, int page, int size, String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        Page<ApiTokenEntity> result = repository.findAll(filter, pageable);
        List<ApiTokenDto> list = result.stream().map(ApiTokenEntity::toDto).toList();
        return new ApiTokenPersistenceAdapter.PageApiToken(list, pageable, result.getTotalElements());
    }

    static class PageApiToken extends PageImpl<ApiTokenDto> {
        public PageApiToken(List<ApiTokenDto> content) {
            super(content);
        }

        public PageApiToken(List<ApiTokenDto> content, Pageable pageable, long total) {
            super(content, pageable, total);
        }
    }

    static class ListApiToken extends ArrayList<ApiTokenDto> {
        public ListApiToken(Collection<? extends ApiTokenDto> c) {
            super(c);
        }
    }
}
