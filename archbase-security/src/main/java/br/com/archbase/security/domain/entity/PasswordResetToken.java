package br.com.archbase.security.domain.entity;

import br.com.archbase.ddd.domain.annotations.DomainEntity;
import br.com.archbase.ddd.domain.base.DomainEntityBase;
import br.com.archbase.validation.fluentvalidator.AbstractArchbaseValidator;
import br.com.archbase.validation.fluentvalidator.context.ArchbaseValidationResult;
import lombok.Builder;
import lombok.Getter;


import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static br.com.archbase.validation.fluentvalidator.predicate.LogicalPredicate.not;
import static br.com.archbase.validation.fluentvalidator.predicate.ObjectPredicate.nullValue;

@Getter
@DomainEntity
public class PasswordResetToken extends DomainEntityBase<User> {
    public static final ChronoUnit EXPIRATION_TIME_UNIT = ChronoUnit.SECONDS;

    private String token;
    private boolean revoked;
    private boolean expired;
    private Long expirationTime;
    private User user;

    @Builder
    public PasswordResetToken(String id, String code, Long version, LocalDateTime updateEntityDate, LocalDateTime createEntityDate, String createdByUser, String lastModifiedByUser, String token, boolean revoked, boolean expired, Long expirationTime, User user) {
        super(id, code, version, updateEntityDate, createEntityDate, createdByUser, lastModifiedByUser);
        this.token = token;
        this.revoked = revoked;
        this.expired = expired;
        this.expirationTime = expirationTime;
        this.user = user;
    }

    public PasswordResetToken(String token, User user) {
        this.token = token;
        this.user = user;
        this.revoked = false;
        this.expired = false;
        this.expirationTime = 3600L;
        this.createdBy(user);
    }

    static class Validator extends AbstractArchbaseValidator<PasswordResetToken> {
        @Override
        public void rules() {
            ruleFor(PasswordResetToken::getToken)
                    .must(not(nullValue()))
                    .withMessage(value->String.format("Token de redefinição de senha deve ser informado, usuário - %s",value.getUser().getUserName()))
                    .withFieldName("token")
                    .critical();
            ruleFor(PasswordResetToken::getUser)
                    .must(not(nullValue()))
                    .withMessage(value->String.format("Usuário deve ser informado",value.getUser().getUserName()))
                    .withFieldName("user")
                    .critical();
        }
    }

    @Override
    public ArchbaseValidationResult validate() {
        return new Validator().validate(this);
    }

    public void updateExpired() {
        if(getCreateEntityDate().plus(expirationTime, EXPIRATION_TIME_UNIT).isBefore(LocalDateTime.now()) && !revoked) {
            expired = true;
        }
    }

    public void revokeToken() {
        updateExpired();
        if (!expired) {
            revoked = true;
        }
    }
}

