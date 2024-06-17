package br.com.archbase.security.domain.dto;

import br.com.archbase.security.domain.entity.ApiToken;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Getter
@Setter
@Builder
@JsonIdentityInfo(generator = ObjectIdGenerators.UUIDGenerator.class, property = "@id")
public class ApiTokenDto {

    protected String id;
    protected String code;
    protected Long version;
    protected LocalDateTime createEntityDate;
    protected LocalDateTime updateEntityDate;
    protected String createdByUser;
    protected String lastModifiedByUser;
    protected String name;
    protected String description;
    protected String token;
    protected UserDto user;
    protected LocalDateTime expirationDate;
    protected boolean revoked;

    public static ApiTokenDto fromDomain(ApiToken apiToken) {
        if (apiToken == null) {
            return null;
        }

        return ApiTokenDto.builder()
                .id(apiToken.getId().toString())
                .code(apiToken.getCode())
                .version(apiToken.getVersion())
                .createEntityDate(apiToken.getCreateEntityDate())
                .updateEntityDate(apiToken.getUpdateEntityDate())
                .createdByUser(apiToken.getCreatedByUser())
                .lastModifiedByUser(apiToken.getLastModifiedByUser())
                .name(apiToken.getName())
                .description(apiToken.getDescription())
                .token(apiToken.getToken())
                .user(apiToken.getUser() != null ? UserDto.fromDomain(apiToken.getUser()):null)
                .expirationDate(apiToken.getExpirationDate())
                .revoked(apiToken.isRevoked())
                .build();
    }

    public ApiToken toDomain() {
        return ApiToken.builder()
                .id(this.id)
                .code(this.code)
                .version(this.version)
                .createEntityDate(this.createEntityDate)
                .updateEntityDate(this.updateEntityDate)
                .lastModifiedByUser(this.lastModifiedByUser)
                .createdByUser(this.createdByUser)
                .name(this.name)
                .description(this.description)
                .user(this.user != null ? this.user.toDomain():null)
                .expirationDate(this.expirationDate)
                .revoked(this.revoked)
                .build();
    }

}
