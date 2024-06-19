package br.com.archbase.security.adapter;

import br.com.archbase.ddd.domain.contracts.FindDataWithFilterQuery;
import br.com.archbase.query.rsql.jpa.SortUtils;
import br.com.archbase.security.domain.dto.AccessTokenDto;
import br.com.archbase.security.domain.dto.ApiTokenDto;
import br.com.archbase.security.persistence.AccessTokenEntity;
import br.com.archbase.security.persistence.ApiTokenEntity;
import br.com.archbase.security.persistence.QAccessTokenEntity;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.AccessTokenJpaRepository;
import br.com.archbase.security.repository.PasswordResetTokenJpaRepository;
import br.com.archbase.security.usecase.AccessTokenUseCase;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AccessTokenPersistenceAdapter implements FindDataWithFilterQuery<String, AccessTokenDto> {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PasswordResetTokenJpaRepository repository;

    @Autowired
    private AccessTokenJpaRepository accessTokenJpaRepository;

    public List<AccessTokenEntity> findAllValidTokenByUser(UserEntity user) {
        QAccessTokenEntity accessToken = QAccessTokenEntity.accessTokenEntity;

        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        BooleanExpression predicate = accessToken.user.id.eq(user.getId())
                .and(accessToken.expired.eq(false).or(accessToken.revoked.eq(false)));

        return queryFactory.selectFrom(accessToken)
                .where(predicate)
                .fetch();
    }

    public AccessTokenEntity findValidTokenByUser(UserEntity user) {
        QAccessTokenEntity accessToken = QAccessTokenEntity.accessTokenEntity;

        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        BooleanExpression predicate = accessToken.user.id.eq(user.getId())
                .and(accessToken.expired.eq(false))
                .and(accessToken.revoked.eq(false));

        return queryFactory.selectFrom(accessToken)
                .where(predicate)
                .fetchFirst();
    }


    @Override
    public AccessTokenDto findById(String id) {
        Optional<AccessTokenEntity> byId = accessTokenJpaRepository.findById(id);
        return byId.map(AccessTokenEntity::toDto).orElse(null);
    }

    @Override
    public Page<AccessTokenDto> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AccessTokenEntity> result = accessTokenJpaRepository.findAll(pageable);
        List<AccessTokenDto> list = result.stream().map(AccessTokenEntity::toDto).toList();
        return new PageAccessToken(list, pageable, result.getTotalElements());
    }

    @Override
    public Page<AccessTokenDto> findAll(int page, int size, String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        Page<AccessTokenEntity> result = accessTokenJpaRepository.findAll(pageable);
        List<AccessTokenDto> list = result.stream().map(AccessTokenEntity::toDto).toList();
        return new PageAccessToken(list, pageable, result.getTotalElements());
    }

    @Override
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
