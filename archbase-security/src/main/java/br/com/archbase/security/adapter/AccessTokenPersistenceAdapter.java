package br.com.archbase.security.adapter;

import br.com.archbase.ddd.domain.contracts.FindDataWithFilterQuery;
import br.com.archbase.query.rsql.jpa.SortUtils;
import br.com.archbase.security.domain.dto.AccessTokenDto;
import br.com.archbase.security.persistence.AccessTokenEntity;
import br.com.archbase.security.persistence.QAccessTokenEntity;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.AccessTokenJpaRepository;
import br.com.archbase.security.repository.PasswordResetTokenJpaRepository;
import br.com.archbase.security.token.TokenUse;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@Slf4j
public class AccessTokenPersistenceAdapter implements FindDataWithFilterQuery<String, AccessTokenDto> {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PasswordResetTokenJpaRepository repository;

    @Autowired
    private AccessTokenJpaRepository accessTokenJpaRepository;

    /**
     * Encontra todos os tokens válidos para um usuário
     * Corrigido: agora retorna apenas tokens não expirados E não revogados
     * Adicionado: verificação da data de expiração real
     */
    @Transactional(readOnly = true)
    public List<AccessTokenEntity> findAllValidTokenByUser(UserEntity user) {
        QAccessTokenEntity accessToken = QAccessTokenEntity.accessTokenEntity;

        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        LocalDateTime now = LocalDateTime.now();

        // Corrigida a lógica do predicado: flags + data de expiração
        BooleanExpression predicate = accessToken.user.id.eq(user.getId())
                .and(accessToken.expired.eq(false))
                .and(accessToken.revoked.eq(false))
                .and(accessToken.expirationDate.after(now)); // ← FIX: Verificar data real

        log.debug("Buscando tokens válidos para usuário {} após {}", user.getEmail(), now);

        return queryFactory.selectFrom(accessToken)
                .where(predicate)
                .orderBy(accessToken.expirationDate.desc())
                .fetch();
    }

    /**
     * Predicado "esta linha é um access token".
     *
     * <p>Inclui {@code TP_USO_TOKEN IS NULL} porque toda linha gravada antes desta versão é access
     * token — a coluna não existia. Sem esta cláusula, atualizar o framework faria toda sessão em
     * curso deixar de ser encontrada e o usuário cairia para 401.
     */
    private BooleanExpression isAccessToken(QAccessTokenEntity accessToken) {
        return accessToken.tokenUse.eq(TokenUse.ACCESS).or(accessToken.tokenUse.isNull());
    }

    /**
     * Encontra um token de <b>acesso</b> válido para um usuário
     * Melhorado: agora ordena por data de expiração para pegar o mais recente
     * Corrigido: verifica data de expiração real além das flags
     */
    @Transactional(readOnly = true)
    public AccessTokenEntity findValidTokenByUser(UserEntity user) {
        QAccessTokenEntity accessToken = QAccessTokenEntity.accessTokenEntity;

        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        LocalDateTime now = LocalDateTime.now();

        BooleanExpression predicate = accessToken.user.id.eq(user.getId())
                .and(accessToken.expired.eq(false))
                .and(accessToken.revoked.eq(false))
                .and(accessToken.expirationDate.after(now)) // ← FIX: Verificar data real
                .and(isAccessToken(accessToken)); // reaproveitar um refresh como access seria confusão de credencial

        AccessTokenEntity result = queryFactory.selectFrom(accessToken)
                .where(predicate)
                .orderBy(accessToken.expirationDate.desc())
                .fetchFirst();

        if (result != null) {
            log.debug("Token válido encontrado para usuário {}: expira em {}",
                    user.getEmail(), result.getExpirationDate());
        } else {
            log.debug("Nenhum token válido encontrado para usuário {} (verificado contra {})",
                    user.getEmail(), now);
        }

        return result;
    }

    /**
     * Encontra um token de <b>acesso</b> pelo valor — é o que o filtro de autenticação consulta.
     * Corrigido: agora retorna apenas tokens não expirados E não revogados
     *
     * @param tokenValue o valor do token JWT
     * @return AccessTokenEntity válido ou null se não encontrado/inválido
     */
    @Transactional(readOnly = true)
    public AccessTokenEntity findTokenByValue(String tokenValue) {
        QAccessTokenEntity accessToken = QAccessTokenEntity.accessTokenEntity;

        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        // Corrigido: adicionar filtros para expired=false e revoked=false
        BooleanExpression predicate = accessToken.token.eq(tokenValue)
                .and(accessToken.expired.eq(false))
                .and(accessToken.revoked.eq(false))
                .and(isAccessToken(accessToken)); // refresh token agora também mora aqui e não vale como credencial

        AccessTokenEntity result = queryFactory.selectFrom(accessToken)
                .where(predicate)
                .orderBy(accessToken.createEntityDate.desc())
                .fetchFirst();

        if (result != null) {
            log.debug("Token válido encontrado: ID={}, Usuário={}, Expiração={}",
                    result.getId(),
                    result.getUser() != null ? result.getUser().getEmail() : "N/A",
                    result.getExpirationDate());
        } else {
            log.debug("Nenhum token válido encontrado para o valor fornecido");
        }

        return result;
    }

    /**
     * Encontra um <b>refresh token</b> vivo pelo valor.
     *
     * <p>É esta consulta que torna o refresh token revogável: antes ele não era persistido, então
     * logout, troca de senha e desativação de conta não tinham o que revogar — o token continuava
     * rendendo pares novos até expirar sozinho.
     *
     * @return a linha correspondente, ou {@code null} se o token não existe, foi revogado ou expirou
     */
    @Transactional(readOnly = true)
    public AccessTokenEntity findRefreshTokenByValue(String tokenValue) {
        QAccessTokenEntity accessToken = QAccessTokenEntity.accessTokenEntity;

        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        BooleanExpression predicate = accessToken.token.eq(tokenValue)
                .and(accessToken.expired.eq(false))
                .and(accessToken.revoked.eq(false))
                .and(accessToken.tokenUse.eq(TokenUse.REFRESH));

        return queryFactory.selectFrom(accessToken)
                .where(predicate)
                .orderBy(accessToken.createEntityDate.desc())
                .fetchFirst();
    }

    /**
     * Encontra tokens expirados que ainda não foram marcados como tal
     */
    @Transactional(readOnly = true)
    public List<AccessTokenEntity> findExpiredButNotMarkedTokens(LocalDateTime now) {
        QAccessTokenEntity accessToken = QAccessTokenEntity.accessTokenEntity;

        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        BooleanExpression predicate = accessToken.expirationDate.before(now)
                .and(accessToken.expired.eq(false));

        List<AccessTokenEntity> result = queryFactory.selectFrom(accessToken)
                .where(predicate)
                .fetch();

        log.debug("Encontrados {} tokens expirados mas não marcados (antes de {})", result.size(), now);
        return result;
    }

    /**
     * Marca automaticamente tokens expirados como expired=true
     * Deve ser chamado periodicamente ou antes de buscar tokens válidos
     */
    @Transactional
    public int markExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        List<AccessTokenEntity> expiredTokens = findExpiredButNotMarkedTokens(now);

        if (!expiredTokens.isEmpty()) {
            log.info("Marcando {} tokens como expirados", expiredTokens.size());
            expiredTokens.forEach(token -> {
                token.setExpired(true);
                log.debug("Token {} marcado como expirado (expirava em {})",
                        token.getId(), token.getExpirationDate());
            });
            accessTokenJpaRepository.saveAll(expiredTokens);
        }

        return expiredTokens.size();
    }

    @Override
    @Transactional(readOnly = true)
    public AccessTokenDto findById(String id) {
        Optional<AccessTokenEntity> byId = accessTokenJpaRepository.findById(id);
        return byId.map(AccessTokenEntity::toDto).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AccessTokenDto> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AccessTokenEntity> result = accessTokenJpaRepository.findAll(pageable);
        List<AccessTokenDto> list = result.stream().map(AccessTokenEntity::toDto).toList();
        return new PageAccessToken(list, pageable, result.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AccessTokenDto> findAll(int page, int size, String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        Page<AccessTokenEntity> result = accessTokenJpaRepository.findAll(pageable);
        List<AccessTokenDto> list = result.stream().map(AccessTokenEntity::toDto).toList();
        return new PageAccessToken(list, pageable, result.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessTokenDto> findAll(List<String> ids) {
        List<AccessTokenEntity> result = accessTokenJpaRepository.findAllById(ids);
        return result.stream().map(AccessTokenEntity::toDto).toList();
    }

    @Override
    public Page<AccessTokenDto> findWithFilter(String filter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AccessTokenEntity> result = accessTokenJpaRepository.findAll(filter, pageable);
        List<AccessTokenDto> list = result.stream().map(AccessTokenEntity::toDto).toList();
        return new PageAccessToken(list, pageable, result.getTotalElements());
    }

    @Override
    public Page<AccessTokenDto> findWithFilter(String filter, int page, int size, String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        Page<AccessTokenEntity> result = accessTokenJpaRepository.findAll(filter, pageable);
        List<AccessTokenDto> list = result.stream().map(AccessTokenEntity::toDto).toList();
        return new PageAccessToken(list, pageable, result.getTotalElements());
    }

    static class PageAccessToken extends PageImpl<AccessTokenDto> {
        public PageAccessToken(List<AccessTokenDto> content) {
            super(content);
        }

        public PageAccessToken(List<AccessTokenDto> content, Pageable pageable, long total) {
            super(content, pageable, total);
        }
    }

    static class ListAccessToken extends ArrayList<AccessTokenDto> {
        public ListAccessToken(Collection<? extends AccessTokenDto> c) {
            super(c);
        }
    }
}