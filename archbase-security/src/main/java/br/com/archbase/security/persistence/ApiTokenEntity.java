package br.com.archbase.security.persistence;

import br.com.archbase.ddd.domain.base.TenantPersistenceEntityBase;
import br.com.archbase.security.domain.dto.ActionDto;
import br.com.archbase.security.domain.dto.ApiTokenDto;
import br.com.archbase.security.domain.dto.ResourceDto;
import br.com.archbase.security.domain.dto.UserDto;
import br.com.archbase.security.domain.entity.Action;
import br.com.archbase.security.domain.entity.ApiToken;
import br.com.archbase.security.domain.entity.Resource;
import br.com.archbase.security.domain.entity.User;
import br.com.archbase.shared.kernel.converters.BooleanToSNConverter;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name="SEGURANCA_TOKEN_API")
@AttributeOverrides({
        @AttributeOverride(name="id",
                column=@Column(name="ID_TOKEN_API")),
        @AttributeOverride(name="code",
                column=@Column(name="CD_TOKEN_API"))
})
public class ApiTokenEntity extends TenantPersistenceEntityBase {

    @Column(name = "NOME", nullable = false)
    private String name;

    @Column(name = "DESCRICAO", nullable = false)
    private String description;

    @ManyToOne
    @JoinColumn(name = "ID_SEGURANCA", nullable = false)
    private UserEntity user;

    @Column(name = "TOKEN", nullable = false)
    private String token;

    @Column(name = "BO_REVOGADO", nullable = false, length = 1)
    @Convert(converter = BooleanToSNConverter.class)
    private Boolean revoked;

    @Column(name = "DH_EXPIRACAO", nullable = false)
    private LocalDateTime expirationDate;

    @Column(name = "BO_ATIVADO", nullable = false, length = 1)
    @Convert(converter = BooleanToSNConverter.class)
    private Boolean activated;

    public ApiTokenEntity() {
    }

    @Builder
    public ApiTokenEntity(String id, String code, Long version, LocalDateTime createEntityDate, String createdByUser, LocalDateTime updateEntityDate, String lastModifiedByUser, String tenantId, String name, String description, UserEntity user, String token, Boolean revoked, LocalDateTime expirationDate, Boolean activated) {
        super(id, code, version, createEntityDate, createdByUser, updateEntityDate, lastModifiedByUser, tenantId);
        this.name = name;
        this.description = description;
        this.user = user;
        this.token = token;
        this.revoked = revoked;
        this.expirationDate = expirationDate;
        this.activated = activated;
    }

    public static ApiTokenEntity fromDomain(ApiToken apiToken) {
        if (apiToken == null) {
            return null;
        }

        ApiTokenEntity actionEntity = new ApiTokenEntity();
        actionEntity.setId(apiToken.getId().toString());
        actionEntity.setCode(apiToken.getCode());
        actionEntity.setVersion(apiToken.getVersion());
        actionEntity.setName(apiToken.getName());
        actionEntity.setDescription(apiToken.getDescription());
        actionEntity.setUser(UserEntity.fromDomain(apiToken.getUser()));
        actionEntity.setToken(apiToken.getToken());
        actionEntity.setRevoked(apiToken.isRevoked());
        actionEntity.setExpirationDate(apiToken.getExpirationDate());
        actionEntity.setActivated(apiToken.isActivated());
        return actionEntity;
    }

    public ApiToken toDomain() {
        User userDomain = this.user != null ? this.user.toDomain() : null;

        return ApiToken.builder()
                .id(this.getId())
                .code(this.getCode())
                .version(this.getVersion())
                .updateEntityDate(this.getUpdateEntityDate())
                .createEntityDate(this.getCreateEntityDate())
                .createdByUser(this.getCreatedByUser())
                .lastModifiedByUser(this.getLastModifiedByUser())
                .name(this.getName())
                .description(this.getDescription())
                .user(userDomain)
                .token(this.getToken())
                .revoked(this.getRevoked())
                .expirationDate(this.getExpirationDate())
                .activated(this.getActivated())
                .build();
    }

    public ApiTokenDto toDto() {
        UserDto userDto = this.user != null ? this.user.toDto() : null;

        return ApiTokenDto.builder()
                .id(this.getId())
                .code(this.getCode())
                .version(this.getVersion())
                .updateEntityDate(this.getUpdateEntityDate())
                .createEntityDate(this.getCreateEntityDate())
                .createdByUser(this.getCreatedByUser())
                .lastModifiedByUser(this.getLastModifiedByUser())
                .name(this.getName())
                .description(this.getDescription())
                .user(userDto)
                .token(this.getToken())
                .revoked(this.getRevoked())
                .expirationDate(this.getExpirationDate())
                .activated(this.getActivated())
                .build();
    }
}
