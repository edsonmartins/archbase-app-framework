package br.com.archbase.security.persistence;

import br.com.archbase.ddd.domain.base.TenantPersistenceEntityBase;
import br.com.archbase.security.domain.dto.AccessTokenDto;
import br.com.archbase.security.domain.dto.UserDto;
import br.com.archbase.security.domain.entity.AccessToken;
import br.com.archbase.security.domain.entity.User;
import br.com.archbase.security.service.ArchbaseJwtService;
import br.com.archbase.security.token.TokenType;
import br.com.archbase.shared.kernel.converters.BooleanToSNConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name="SEGURANCA_TOKEN_ACESSO")
@Getter
@Setter
@AttributeOverrides({
        @AttributeOverride(name="id",
                column=@Column(name="ID_TOKEN")),
        @AttributeOverride(name="code",
                column=@Column(name="CD_TOKEN"))
})
public class AccessTokenEntity extends TenantPersistenceEntityBase {

  @Column(name="TOKEN", length = 5000, unique = true)
  private String token;

  @Enumerated(EnumType.STRING)
  @Column(name="TP_TOKEN", length = 50)
  private TokenType tokenType = TokenType.BEARER;

  @Setter
  @Convert(converter = BooleanToSNConverter.class)
  @Column(name="TOKEN_REVOGADO", length = 1)
  private boolean revoked;

  @Setter
  @Convert(converter = BooleanToSNConverter.class)
  @Column(name="TOKEN_EXPIRADO", length = 1)
  private boolean expired;

  @Column(name="TEMPO_EXPIRACAO")
  private Long expirationTime;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name="DH_EXPIRACAO")
  private LocalDateTime expirationDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ID_USUARIO")
  private UserEntity user;

  @Transient
  @JsonIgnore
  @Autowired
  private ArchbaseJwtService jwtService;

  public AccessTokenEntity() {
    super();
  }

  @Builder
  public AccessTokenEntity(String id, String code, Long version, LocalDateTime createEntityDate, String createdByUser, LocalDateTime updateEntityDate, String lastModifiedByUser, String tenantId, String token, TokenType tokenType, boolean revoked, boolean expired, Long expirationTime, LocalDateTime expirationDate, UserEntity user, ArchbaseJwtService jwtService) {
    super(id, code, version, createEntityDate, createdByUser, updateEntityDate, lastModifiedByUser, tenantId);
    this.token = token;
    this.tokenType = tokenType;
    this.revoked = revoked;
    this.expired = expired;
    this.expirationTime = expirationTime;
    this.expirationDate = expirationDate;
    this.user = user;
    this.jwtService = jwtService;
  }

  public static AccessTokenEntity fromDomain(AccessToken accessToken) {
    if (accessToken == null) {
      return null;
    }

    AccessTokenEntity accessTokenEntity = new AccessTokenEntity();
    accessTokenEntity.setId(accessToken.getId().toString());
    accessTokenEntity.setCode(accessToken.getCode());
    accessTokenEntity.setVersion(accessToken.getVersion());
    accessTokenEntity.setExpired(accessToken.isExpired());
    accessTokenEntity.setRevoked(accessToken.isRevoked());
    accessTokenEntity.setUser(UserEntity.fromDomain(accessToken.getUser()));
    accessTokenEntity.setToken(accessToken.getToken());
    accessTokenEntity.setTokenType(accessToken.getTokenType());
    accessTokenEntity.setExpirationTime(accessToken.getExpirationTime());
    accessTokenEntity.setExpirationDate(accessToken.getExpirationDate());
    return accessTokenEntity;
  }

  public AccessToken toDomain() {
    User userDomain = this.user != null ? this.user.toDomain() : null;

    return AccessToken.builder()
            .id(this.getId())
            .code(this.getCode())
            .version(this.getVersion())
            .updateEntityDate(this.getUpdateEntityDate())
            .createEntityDate(this.getCreateEntityDate())
            .createdByUser(this.getCreatedByUser())
            .lastModifiedByUser(this.getLastModifiedByUser())
            .token(this.getToken())
            .expirationTime(this.getExpirationTime())
            .expirationDate(this.getExpirationDate())
            .revoked(this.isRevoked())
            .expired(this.isExpired())
            .tokenType(this.getTokenType())
            .user(userDomain)
            .build();
  }

  public AccessTokenDto toDto() {
    UserDto userDto = this.user != null ? this.user.toDto() : null;

    return AccessTokenDto.builder()
            .id(this.getId())
            .code(this.getCode())
            .version(this.getVersion())
            .updateEntityDate(this.getUpdateEntityDate())
            .createEntityDate(this.getCreateEntityDate())
            .createdByUser(this.getCreatedByUser())
            .lastModifiedByUser(this.getLastModifiedByUser())
            .token(this.getToken())
            .expirationTime(this.getExpirationTime())
            .expirationDate(this.expirationDate)
            .revoked(this.isRevoked())
            .expired(this.isExpired())
            .tokenType(this.getTokenType())
            .user(userDto)
            .build();
  }
}
