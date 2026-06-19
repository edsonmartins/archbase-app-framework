package br.com.archbase.security.repository;


import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import br.com.archbase.security.persistence.UserEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface UserJpaRepository extends ArchbaseCommonJpaRepository<UserEntity, String, Long> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Verifica se existe usuário por email IGNORANDO o filtro de tenant.
     * Usado para descobrir tenants disponíveis para um email antes do login.
     *
     * A tabela SEGURANCA usa herança SINGLE_TABLE com TP_SEGURANCA como discriminador
     *
     * @param email Email do usuário
     * @return true se existe algum usuário com esse email em qualquer tenant
     */
    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END FROM SEGURANCA WHERE TP_SEGURANCA = 'USUARIO' AND EMAIL = :email", nativeQuery = true)
    boolean existsByEmailIgnoringTenant(@Param("email") String email);

    /**
     * Lista os tenants disponíveis para um email IGNORANDO o filtro de tenant.
     * Queries nativas ignoram o @Filter do Hibernate, portanto enxergam todos os tenants.
     * Usado para descobrir os tenants disponíveis para um email antes do login.
     *
     * @param email Email do usuário
     * @return Linhas {@code [tenantId, nome, descricao]} de cada usuário com esse email em qualquer tenant.
     *         Retorna {@code Object[]} (mapeado por índice no serviço) em vez de projeção de interface:
     *         no Postgres o alias não-quotado é rebaixado para minúsculas e o matching por nome da
     *         projeção falha (erro 500). Object[] mapeia por posição e é imune a isso.
     */
    @Query(value = "SELECT TENANT_ID, NOME, DESCRICAO FROM SEGURANCA WHERE TP_SEGURANCA = 'USUARIO' AND EMAIL = :email AND TENANT_ID IS NOT NULL", nativeQuery = true)
    List<Object[]> findTenantsByEmailIgnoringTenant(@Param("email") String email);
}
