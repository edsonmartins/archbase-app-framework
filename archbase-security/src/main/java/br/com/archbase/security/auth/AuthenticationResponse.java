package br.com.archbase.security.auth;


import br.com.archbase.security.domain.entity.User;
import br.com.archbase.security.token.TokenType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {

  @JsonProperty("access_token")
  private String accessToken;
  @JsonProperty("refresh_token")
  private String refreshToken;
  @JsonProperty("expires_in")
  private Long expirationTime;
  @JsonProperty("id_token")
  private String id;
  @JsonProperty("token_type")
  private TokenType tokenType;
  @JsonProperty("user")
  private User user;
  @JsonProperty("context")
  private Object context;

  /** Sinaliza que a senha conferiu mas falta o 2º fator (MFA); os tokens ainda não vêm. */
  @JsonProperty("mfa_required")
  private Boolean mfaRequired;
  /** Token curto de desafio para completar o MFA em {@code POST /auth/mfa/verify}. */
  @JsonProperty("challenge_token")
  private String challengeToken;

}
