package br.com.archbase.security.persistence;

import br.com.archbase.ddd.domain.base.TenantPersistenceEntityBase;
import br.com.archbase.security.token.TokenType;
import br.com.archbase.shared.kernel.converters.BooleanToSNConverter;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="SEGURANCA_TOKEN_ACESSO")
@Getter
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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ID_USUARIO")
  private UserEntity user;

  public AccessTokenEntity() {
    super();
  }

  @Builder
  public AccessTokenEntity(String id, String code, String token, TokenType tokenType, boolean revoked, boolean expired, Long expirationTime, UserEntity user) {
    super(id, code);
    this.token = token;
    this.tokenType = tokenType;
    this.revoked = revoked;
    this.expired = expired;
    this.expirationTime = expirationTime;
    this.user = user;
  }
}
