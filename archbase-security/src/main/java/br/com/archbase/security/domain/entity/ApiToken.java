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
    /**
     * Valor em claro. {@code null} para token criado a partir da 3.0.11 lido do banco — ele só
     * existe no instante da criação. Ver {@link #tokenHash}.
     */
    protected String token;
    /**
     * SHA-256 do token. Carregado no domínio para que a conversão entidade↔domínio não o perca e
     * uma gravação de volta apague a credencial da linha. Não é exposto em {@code ApiTokenDto}.
     */
    protected String tokenHash;
    protected User user;
    protected LocalDateTime expirationDate;
    protected boolean revoked;
    protected boolean activated;
    /**
     * Tenant dono do token. Necessário no filtro de autenticação para aplicar ao token de API o
     * mesmo isolamento tenant↔credencial que o JWT já tinha pelo claim assinado.
     */
    protected String tenantId;

    @Builder
    public ApiToken(String id, String code, Long version, LocalDateTime updateEntityDate, LocalDateTime createEntityDate, String createdByUser, String lastModifiedByUser, String name, String description, String token, String tokenHash, User user, LocalDateTime expirationDate, boolean revoked, boolean activated, String tenantId) {
        super(id, code, version, updateEntityDate, createEntityDate, createdByUser, lastModifiedByUser);
        this.name = name;
        this.description = description;
        this.token = token;
        this.tokenHash = tokenHash;
        this.user = user;
        this.expirationDate = expirationDate;
        this.revoked = revoked;
        this.activated = activated;
        this.tenantId = tenantId;
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
