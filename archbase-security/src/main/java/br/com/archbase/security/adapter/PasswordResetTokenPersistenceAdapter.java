package br.com.archbase.security.adapter;

import br.com.archbase.security.domain.entity.PasswordResetToken;
import br.com.archbase.security.persistence.PasswordResetTokenEntity;
import br.com.archbase.security.persistence.QPasswordResetTokenEntity;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.PasswordResetTokenJpaRepository;
import br.com.archbase.security.usecase.PasswordResetTokenUseCase;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PasswordResetTokenPersistenceAdapter implements PasswordResetTokenUseCase {

    private final EntityManager entityManager;

    private final PasswordResetTokenJpaRepository repository;

    public List<PasswordResetToken> findAllNonExpiredAndNonRevokedTokens(UserEntity user) {
        QPasswordResetTokenEntity passwordResetToken = QPasswordResetTokenEntity.passwordResetTokenEntity;

        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        BooleanExpression predicate = passwordResetToken.revoked.isFalse()
                .and(passwordResetToken.expired.isFalse())
                .and(passwordResetToken.user.id.eq(user.getId()));
        List<PasswordResetTokenEntity> tokens = queryFactory.selectFrom(passwordResetToken)
                .where(predicate)
                .fetch();
        return tokens.stream().map(PasswordResetTokenEntity::toDomain).toList();
    }

    public PasswordResetToken findToken(UserEntity user, String token) {
        QPasswordResetTokenEntity passwordResetToken = QPasswordResetTokenEntity.passwordResetTokenEntity;

        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        BooleanExpression predicate = passwordResetToken.user.id.eq(user.getId())
                .and(passwordResetToken.token.eq(token));

        PasswordResetTokenEntity tokenEntity = queryFactory.selectFrom(passwordResetToken)
                .where(predicate)
                .fetchFirst();
        return tokenEntity != null ? tokenEntity.toDomain() : null;
    }

    public void saveAll(List<PasswordResetToken> passwordResetTokens) {
        if (passwordResetTokens != null && !passwordResetTokens.isEmpty()) {
            List<PasswordResetTokenEntity> tokens = passwordResetTokens.stream().map(PasswordResetTokenEntity::fromDomain).toList();
            repository.saveAll(tokens);
        }
    }

    public void save(PasswordResetToken passwordResetToken) {
        repository.save(PasswordResetTokenEntity.fromDomain(passwordResetToken));
    }
}
