package br.com.archbase.security.domain.entity;

import br.com.archbase.ddd.domain.annotations.DomainAggregateRoot;
import br.com.archbase.ddd.domain.annotations.DomainEntity;
import br.com.archbase.ddd.domain.base.DomainAggregatorBase;
import br.com.archbase.ddd.domain.base.DomainEntityBase;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.token.TokenType;
import br.com.archbase.validation.fluentvalidator.AbstractArchbaseValidator;
import br.com.archbase.validation.fluentvalidator.context.ArchbaseValidationResult;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@DomainEntity
public class AccessToken extends DomainAggregatorBase<AccessToken> {

    protected String token;
    protected TokenType tokenType = TokenType.BEARER;
    protected boolean revoked;
    protected boolean expired;
    protected boolean activated; // Novo campo para verificar se o token foi ativado
    protected Long expirationTime;
    protected LocalDateTime expirationDate;
    protected User user;

    @Builder
    public AccessToken(String id, String code, Long version, LocalDateTime updateEntityDate, LocalDateTime createEntityDate, String createdByUser, String lastModifiedByUser, String token, TokenType tokenType, boolean revoked, boolean expired, boolean activated, Long expirationTime, LocalDateTime expirationDate, User user) {
        super(id, code, version, updateEntityDate, createEntityDate, createdByUser, lastModifiedByUser);
        this.token = token;
        this.tokenType = tokenType;
        this.revoked = revoked;
        this.expired = expired;
        this.activated = activated; // Inicializar o campo
        this.expirationTime = expirationTime;
        this.expirationDate = expirationDate;
        this.user = user;
    }

    static class Validator extends AbstractArchbaseValidator<AccessToken> {
        @Override
        public void rules() {

        }
    }

    @Override
    public ArchbaseValidationResult validate() {
        return new Validator().validate(this);
    }
}
