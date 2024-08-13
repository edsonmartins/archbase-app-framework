package br.com.archbase.security.domain.dto;

import br.com.archbase.security.domain.entity.AccessToken;
import br.com.archbase.security.token.TokenType;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.UUIDGenerator.class, property = "@id")
public class AccessTokenDto  {

  protected String id;
  protected String code;
  protected Long version;
  protected LocalDateTime createEntityDate;
  protected LocalDateTime updateEntityDate;
  protected String createdByUser;
  protected String lastModifiedByUser;
  protected String token;
  protected TokenType tokenType = TokenType.BEARER;
  protected boolean revoked;
  protected boolean expired;
  protected Long expirationTime;
  protected LocalDateTime expirationDate;
  protected UserDto user;



  public static AccessTokenDto fromDomain(AccessToken accessToken) {
    if (accessToken == null) {
      return null;
    }

    return AccessTokenDto.builder()
            .id(accessToken.getId().toString())
            .code(accessToken.getCode())
            .version(accessToken.getVersion())
            .createEntityDate(accessToken.getCreateEntityDate())
            .updateEntityDate(accessToken.getUpdateEntityDate())
            .createdByUser(accessToken.getCreatedByUser())
            .lastModifiedByUser(accessToken.getLastModifiedByUser())
            .token(accessToken.getToken())
            .tokenType(accessToken.getTokenType())
            .revoked(accessToken.isRevoked())
            .expired(accessToken.isExpired())
            .expirationTime(accessToken.getExpirationTime())
            .user(UserDto.fromDomain(accessToken.getUser()))
            .build();
  }

  public static LocalDateTime convertToLocalDateTimeViaMilisecond(Date dateToConvert) {
    return Instant.ofEpochMilli(dateToConvert.getTime())
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();
  }

  public AccessToken toDomain() {
    return AccessToken.builder()
            .id(this.id)
            .code(this.code)
            .version(this.version)
            .createEntityDate(this.createEntityDate)
            .updateEntityDate(this.updateEntityDate)
            .lastModifiedByUser(this.lastModifiedByUser)
            .createdByUser(this.createdByUser)
            .token(this.token)
            .tokenType(this.tokenType)
            .revoked(this.revoked)
            .expired(this.expired)
            .expirationTime(this.expirationTime)
            .user(this.user != null ? this.user.toDomain() : null)
            .build();
  }

}
