package br.com.archbase.security.adapter;

import br.com.archbase.security.persistence.AccessTokenEntity;
import br.com.archbase.security.persistence.QAccessTokenEntity;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.PasswordResetTokenJpaRepository;
import br.com.archbase.security.usecase.AccessTokenUseCase;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AccessTokenPersistenceAdapter implements AccessTokenUseCase {

    private final EntityManager entityManager;

    private final PasswordResetTokenJpaRepository repository;

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
}
