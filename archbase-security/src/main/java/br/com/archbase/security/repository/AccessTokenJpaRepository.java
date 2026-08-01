package br.com.archbase.security.repository;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import br.com.archbase.security.persistence.AccessTokenEntity;
import org.springframework.data.jpa.repository.Modifying;
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
   * Revoga, em um único comando, todos os tokens vivos do usuário dono do token informado.
   *
   * <p>Existe para o logout. Carregar as entidades e salvá-las uma a uma tinha dois problemas:
   * exigia navegar a associação LAZY do usuário fora de sessão, e o {@code @Version} herdado fazia
   * um refresh concorrente derrubar toda a transação por conflito otimista — desfazendo inclusive a
   * revogação do token que o usuário acabou de apresentar, com o cliente lendo a resposta como
   * logout bem-sucedido. O update em lote não toca associação nem versão.
   */
  @Modifying
  @Query("UPDATE AccessTokenEntity t SET t.expired = true, t.revoked = true "
          + "WHERE t.expired = false AND t.revoked = false "
          + "AND t.user.id = (SELECT o.user.id FROM AccessTokenEntity o WHERE o.token = :token)")
  int revokeAllTokensOfOwnerOf(@Param("token") String token);

  /**
   * Conta a quantidade de tokens válidos para um usuário
   */
  @Query("SELECT COUNT(t) FROM AccessTokenEntity t WHERE t.user.id = :userId " +
          "AND t.expired = false AND t.revoked = false")
  long countValidTokensByUserId(@Param("userId") String userId);
}
