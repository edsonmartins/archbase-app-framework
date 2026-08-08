package br.com.archbase.security.repository;


import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import br.com.archbase.security.persistence.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface UserJpaRepository extends ArchbaseCommonJpaRepository<UserEntity, String, Long> {

    Optional<UserEntity> findByEmail(String email);

    /**
     * Carrega o usuário com grupos e perfil já materializados.
     *
     * <p>Existe para o diagnóstico e a simulação, que montam um {@code AccessSubject} fora do
     * escopo transacional de uma requisição autenticada. Sem o grafo, tocar
     * {@code getGroups()} ali é {@code LazyInitializationException}.
     */
    @EntityGraph(attributePaths = {"groups", "groups.group", "profile"})
    @Query("SELECT u FROM UserEntity u WHERE u.id = :id")
    Optional<UserEntity> findByIdWithGroupsAndProfile(@Param("id") String id);

    /**
     * Quantos administradores existem no tenant.
     *
     * <p>Consulta de contagem, e não {@code findAll().stream().filter().count()}: o painel de
     * diagnóstico serve justamente a sistemas já com problema, e materializar a tabela inteira de
     * usuários para contar quatro linhas é o tipo de coisa que derruba o diagnóstico junto.
     */
    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.isAdministrator = true")
    long countAdministrators();

    /**
     * Quem está no grupo, já com grupos e perfil materializados.
     *
     * <p>O {@code EntityGraph} não é detalhe de desempenho: cada membro vira um
     * {@code AccessSubject} para que o painel some o que ele acumula de <b>todas</b> as origens —
     * e montar o sujeito tocando associação lazy fora de transação é
     * {@code LazyInitializationException}, o mesmo defeito que apareceu no logout.
     */
    @EntityGraph(attributePaths = {"groups", "groups.group", "profile"})
    @Query("SELECT DISTINCT u FROM UserEntity u JOIN u.groups ug "
            + "WHERE ug.group.id = :groupId ORDER BY u.name")
    java.util.List<UserEntity> findMembersOfGroup(@Param("groupId") String groupId);

    /** Ramo "Pessoas" da árvore, paginado e filtrado no servidor. */
    @Query("SELECT u FROM UserEntity u "
            + "WHERE (:filtro IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :filtro, '%')) "
            + "   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :filtro, '%'))) "
            + "ORDER BY u.name")
    Page<UserEntity> findForTree(@Param("filtro") String filtro, Pageable pageable);

    /** Quem tem este perfil, com grupos e perfil materializados — mesmo motivo de findMembersOfGroup. */
    @EntityGraph(attributePaths = {"groups", "groups.group", "profile"})
    @Query("SELECT u FROM UserEntity u WHERE u.profile.id = :profileId ORDER BY u.name")
    java.util.List<UserEntity> findMembersOfProfile(@Param("profileId") String profileId);

    /**
     * Os administradores, para a consulta reversa de "quem alcança".
     *
     * <p>Eles não têm concessão nenhuma e alcançam tudo: sem esta lista, a resposta à pergunta
     * "quem pode fazer isto?" ficaria errada exatamente para as contas que mais importam numa
     * auditoria.
     */
    @Query("SELECT u FROM UserEntity u WHERE u.isAdministrator = true ORDER BY u.name")
    java.util.List<UserEntity> findAllAdministrators();

    /** Os itens por trás de {@link #countAdministrators()}. */
    @Query("SELECT u FROM UserEntity u WHERE u.isAdministrator = true ORDER BY u.name")
    Page<UserEntity> findAdministrators(Pageable pageable);

    /** Idem, por e-mail — o identificador que quem opera o admin tem em mãos. */
    @EntityGraph(attributePaths = {"groups", "groups.group", "profile"})
    @Query("SELECT u FROM UserEntity u WHERE u.email = :email")
    Optional<UserEntity> findByEmailWithGroupsAndProfile(@Param("email") String email);

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
    // Nome de tabela em minúsculas: no MySQL sobre Linux os identificadores são case-sensitive
    // (lower_case_table_names=0) e o Hibernate cria `seguranca`, então `SEGURANCA` não é encontrada.
    // PostgreSQL e H2 dobram para minúsculas e escondem o problema.
    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END FROM seguranca WHERE TP_SEGURANCA = 'USUARIO' AND EMAIL = :email", nativeQuery = true)
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
    // Minúsculas pelo mesmo motivo da consulta acima (case-sensitivity no MySQL/Linux).
    @Query(value = "SELECT TENANT_ID, NOME, DESCRICAO FROM seguranca WHERE TP_SEGURANCA = 'USUARIO' AND EMAIL = :email AND TENANT_ID IS NOT NULL", nativeQuery = true)
    List<Object[]> findTenantsByEmailIgnoringTenant(@Param("email") String email);
}
