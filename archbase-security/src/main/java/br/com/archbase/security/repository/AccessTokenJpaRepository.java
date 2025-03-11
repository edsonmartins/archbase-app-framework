package br.com.archbase.security.repository;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import br.com.archbase.security.persistence.AccessTokenEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccessTokenJpaRepository extends ArchbaseCommonJpaRepository<AccessTokenEntity, String, Long> {

  /**
   * Busca um token por seu valor
   */
  Optional<AccessTokenEntity> findByToken(String token);

  /**
   * Busca todos os tokens válidos de um usuário
   */
  @Query("SELECT t FROM AccessTokenEntity t WHERE t.user.id = :userId " +
          "AND t.expired = false AND t.revoked = false " +
          "ORDER BY t.expirationDate DESC")
  List<AccessTokenEntity> findAllValidTokensByUserId(@Param("userId") String userId);

  /**
   * Busca tokens expirados mas não marcados como tal
   */
  List<AccessTokenEntity> findByExpirationDateBeforeAndExpiredFalse(LocalDateTime dateTime);

  /**
   * Busca um token válido pelo ID do usuário
   */
  @Query("SELECT t FROM AccessTokenEntity t WHERE t.user.id = :userId " +
          "AND t.expired = false AND t.revoked = false " +
          "ORDER BY t.expirationDate DESC")
  Optional<AccessTokenEntity> findValidTokenByUserId(@Param("userId") String userId);

  /**
   * Busca tokens expirados mais antigos que uma data especificada
   * Útil para limpeza de tokens muito antigos
   */
  @Query("SELECT t FROM AccessTokenEntity t WHERE t.expirationDate < :date")
  List<AccessTokenEntity> findExpiredTokensOlderThan(@Param("date") LocalDateTime date);

  /**
   * Conta a quantidade de tokens válidos para um usuário
   */
  @Query("SELECT COUNT(t) FROM AccessTokenEntity t WHERE t.user.id = :userId " +
          "AND t.expired = false AND t.revoked = false")
  long countValidTokensByUserId(@Param("userId") String userId);
}
