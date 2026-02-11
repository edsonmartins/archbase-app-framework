package br.com.archbase.security.repository;


import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import br.com.archbase.security.persistence.UserEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
