package br.com.archbase.security.persistence;

import br.com.archbase.ddd.domain.base.TenantPersistenceEntityBase;
import br.com.archbase.security.domain.entity.PasswordResetToken;
import br.com.archbase.shared.kernel.converters.BooleanToSNConverter;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="SEGURANCA_TOKEN_REDEFINICAO_SENHA")
@Getter
@Setter
@AttributeOverrides({
        @AttributeOverride(name="id",
                column=@Column(name="ID_TOKEN")),
        @AttributeOverride(name="code",
                column=@Column(name="CD_TOKEN"))
})
public class PasswordResetTokenEntity extends TenantPersistenceEntityBase {

  @Column(name="TOKEN", length = 13)
  private String token;

  @Convert(converter = BooleanToSNConverter.class)
  @Column(name="TOKEN_REVOGADO", length = 1)
  private boolean revoked;

  @Convert(converter = BooleanToSNConverter.class)
  @Column(name="TOKEN_EXPIRADO", length = 1)
  private boolean expired;

  @Column(name="TEMPO_EXPIRACAO")
  private Long expirationTime;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ID_USUARIO")
  private UserEntity user;

  public PasswordResetTokenEntity() {
    super();
  }

  @Builder
  public PasswordResetTokenEntity(String id, String code, String token, boolean revoked, Long expirationTime, UserEntity user) {
    super(id, code);
    this.token = token;
    this.revoked = revoked;
    this.expirationTime = expirationTime;
    this.user = user;
  }

  public static PasswordResetTokenEntity fromDomain(PasswordResetToken passwordResetToken) {
    if (passwordResetToken == null) {
      return null;
    }

    PasswordResetTokenEntity tokenEntity = new PasswordResetTokenEntity();
    tokenEntity.setId(passwordResetToken.getId().toString());
    tokenEntity.setCode(passwordResetToken.getCode());
    tokenEntity.setVersion(passwordResetToken.getVersion());
    tokenEntity.setUpdateEntityDate(passwordResetToken.getUpdateEntityDate());
    tokenEntity.setCreateEntityDate(passwordResetToken.getCreateEntityDate());
    tokenEntity.setCreatedByUser(passwordResetToken.getCreatedByUser());
    tokenEntity.setLastModifiedByUser(passwordResetToken.getLastModifiedByUser());

    tokenEntity.setToken(passwordResetToken.getToken());
    tokenEntity.setRevoked(passwordResetToken.isRevoked());
    tokenEntity.setExpired(passwordResetToken.isExpired());
    tokenEntity.setExpirationTime(passwordResetToken.getExpirationTime());
    tokenEntity.setUser(passwordResetToken.getUser() != null ? UserEntity.fromDomain(passwordResetToken.getUser()) : null);

    return tokenEntity;
  }

  public PasswordResetToken toDomain() {
    return PasswordResetToken.builder()
            .id(this.getId())
            .code(this.getCode())
            .version(this.getVersion())
            .updateEntityDate(this.getUpdateEntityDate())
            .createEntityDate(this.getCreateEntityDate())
            .createdByUser(this.getCreatedByUser())
            .lastModifiedByUser(this.getLastModifiedByUser())
            .token(this.getToken())
            .revoked(this.isRevoked())
            .expired(this.isExpired())
            .expirationTime(this.getExpirationTime())
            .user(this.getUser() != null ? this.getUser().toDomain() : null)
            .build();
  }
}
