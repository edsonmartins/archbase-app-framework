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
   * Id do usuário dono do token informado, sem carregar a entidade.
   *
   * <p>Projeção em vez de {@code findByToken(...).getUser().getId()}: o logout roda na cadeia de
   * filtros, fora do {@code OpenEntityManagerInView}, e navegar a associação LAZY ali estoura
   * {@code LazyInitializationException}.
   */
  @Query("SELECT t.user.id FROM AccessTokenEntity t WHERE t.token = :token")
  Optional<String> findOwnerIdByToken(@Param("token") String token);

  /**
   * Revoga, em um único comando, todos os tokens vivos de um usuário.
   *
   * <p>Update em lote, e não carregar-e-salvar entidade a entidade, porque o {@code @Version}
   * herdado fazia um refresh concorrente derrubar toda a transação do logout por conflito otimista
   * — desfazendo inclusive a revogação do token recém-apresentado, com o cliente lendo a resposta
   * como logout bem-sucedido.
   *
   * <p>Recebe o {@code userId} já resolvido em vez de derivá-lo por subconsulta sobre esta mesma
   * tabela: {@code UPDATE ... WHERE x = (SELECT ... FROM a_mesma_tabela)} é recusado pelo MySQL e
   * pelo MariaDB (ERROR 1093), e o framework não pode assumir PostgreSQL aqui.
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE AccessTokenEntity t SET t.expired = true, t.revoked = true "
          + "WHERE t.expired = false AND t.revoked = false AND t.user.id = :userId")
  int revokeAllTokensOfUser(@Param("userId") String userId);

  /**
   * Conta a quantidade de tokens válidos para um usuário
   */
  @Query("SELECT COUNT(t) FROM AccessTokenEntity t WHERE t.user.id = :userId " +
          "AND t.expired = false AND t.revoked = false")
  long countValidTokensByUserId(@Param("userId") String userId);
}
