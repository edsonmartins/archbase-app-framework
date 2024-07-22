package br.com.archbase.security.domain.entity;

import br.com.archbase.ddd.domain.annotations.DomainAggregateRoot;
import br.com.archbase.ddd.domain.base.DomainAggregatorBase;
import br.com.archbase.ddd.domain.contracts.ValidationResult;
import br.com.archbase.validation.fluentvalidator.AbstractArchbaseValidator;
import br.com.archbase.validation.fluentvalidator.context.ArchbaseValidationResult;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Getter
@Setter
@DomainAggregateRoot
public class ApiToken extends DomainAggregatorBase<ApiToken> {

    protected String name;
    protected String description;
    protected String token;
    protected User user;
    protected LocalDateTime expirationDate;
    protected boolean revoked;
    protected boolean activated;

    @Builder
    public ApiToken(String id, String code, Long version, LocalDateTime updateEntityDate, LocalDateTime createEntityDate, String createdByUser, String lastModifiedByUser, String name, String description, String token, User user, LocalDateTime expirationDate, boolean revoked, boolean activated) {
        super(id, code, version, updateEntityDate, createEntityDate, createdByUser, lastModifiedByUser);
        this.name = name;
        this.description = description;
        this.token = token;
        this.user = user;
        this.expirationDate = expirationDate;
        this.revoked = revoked;
        this.activated = activated;
    }

    static class Validator extends AbstractArchbaseValidator<ApiToken> {
        @Override
        public void rules() {

        }
    }

    @Override
    public ValidationResult validate() {
        return new ApiToken.Validator().validate(this);
    }

}
