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
   *
   * <p><b>Query nativa de propósito.</b> {@code AccessTokenEntity} é {@code @TenantId}, e o
   * {@code LogoutFilter} do Spring roda <b>antes</b> do {@code ArchbaseJwtAuthenticationFilter} —
   * ou seja, com o {@code ArchbaseTenantContext} ainda vazio. Em JPQL o Hibernate aplicava o
   * discriminador com o tenant padrão, a busca não encontrava a linha de nenhum outro tenant, o
   * serviço retornava cedo e o logout respondia 200 <b>sem revogar nada</b>. O token é uma cadeia
   * aleatória globalmente única: procurá-lo sem recorte de tenant é correto e é o único jeito de
   * o logout funcionar antes de haver contexto.
   */
  @Query(value = "SELECT id_usuario FROM seguranca_token_acesso WHERE token = :token",
          nativeQuery = true)
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
   *
   * <p><b>Nativa pelo mesmo motivo de {@link #findOwnerIdByToken(String)}</b>: o logout executa
   * antes de haver tenant no contexto, e em JPQL o discriminador recortaria o UPDATE para o tenant
   * padrão — revogando zero linha e devolvendo 200. O id do usuário já vem resolvido e é único,
   * então o comando não precisa do recorte para ser correto.
   *
   * <p><b>Tudo em minúsculas</b>, como as demais queries nativas deste projeto: o MySQL em Linux
   * diferencia maiúsculas em nome de tabela, e a versão em caixa alta falhava lá com
   * "Table 'SEGURANCA_TOKEN_ACESSO' doesn't exist" — passando em H2 e PostgreSQL.
   *
   * <p>Os literais são {@code 'S'}/{@code 'N'} e não booleanos: as colunas passam pelo
   * {@code BooleanToSNConverter}, que SQL nativo não aplica. Escrever {@code true} aqui gravaria
   * um valor que a leitura por JPA interpretaria como falso — a revogação sumiria na próxima
   * consulta.
   */
  @Modifying(clearAutomatically = false, flushAutomatically = true)
  @Query(value = "UPDATE seguranca_token_acesso SET token_expirado = 'S', token_revogado = 'S' "
          + "WHERE token_expirado = 'N' AND token_revogado = 'N' AND id_usuario = :userId",
          nativeQuery = true)
  int revokeAllTokensOfUser(@Param("userId") String userId);

  /**
   * Revoga um único token, o apresentado.
   *
   * <p>É a rotação da renovação: quem renova invalida o refresh que usou e recebe um par novo. O
   * escopo é uma sessão só — as demais sessões do mesmo usuário não têm nada a ver com esta
   * renovação e não podem ser derrubadas por ela.
   *
   * <p><b>Nativa e em lote</b> pelas mesmas razões de {@link #revokeAllTokensOfUser(String)}:
   * carregar a entidade e salvar faz um refresh concorrente derrubar a transação inteira por
   * conflito no {@code @Version} herdado. Literais {@code 'S'}/{@code 'N'} por causa do
   * {@code BooleanToSNConverter}, que SQL nativo não aplica, e tudo em minúsculas porque o MySQL
   * em Linux diferencia maiúsculas em nome de tabela.
   */
  @Modifying(clearAutomatically = false, flushAutomatically = true)
  @Query(value = "UPDATE seguranca_token_acesso SET token_expirado = 'S', token_revogado = 'S' "
          + "WHERE token_expirado = 'N' AND token_revogado = 'N' AND token = :token",
          nativeQuery = true)
  int revokeTokenByValue(@Param("token") String token);

  /**
   * Revoga todos os refresh tokens vivos de um usuário, preservando os access tokens.
   *
   * <p>Serve ao login de quem NÃO pode ter múltiplas sessões: o access ainda válido é reaproveitado
   * e o refresh é reemitido, então os refresh anteriores precisam morrer para não acumularem.
   *
   * <p><b>Nativa e em lote</b> pelo mesmo motivo das irmãs acima — o carregar-e-salvar entidade a
   * entidade expunha a operação ao conflito otimista de um refresh concorrente.
   */
  @Modifying(clearAutomatically = false, flushAutomatically = true)
  @Query(value = "UPDATE seguranca_token_acesso SET token_expirado = 'S', token_revogado = 'S' "
          + "WHERE token_expirado = 'N' AND token_revogado = 'N' AND id_usuario = :userId "
          + "AND tp_uso_token = 'REFRESH'",
          nativeQuery = true)
  int revokeAllRefreshTokensOfUser(@Param("userId") String userId);

  /**
   * Conta a quantidade de tokens válidos para um usuário
   */
  @Query("SELECT COUNT(t) FROM AccessTokenEntity t WHERE t.user.id = :userId " +
          "AND t.expired = false AND t.revoked = false")
  long countValidTokensByUserId(@Param("userId") String userId);
}
