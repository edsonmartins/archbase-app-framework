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

  /**
   * Tenant em que este login foi autenticado — a linha de usuário que conferiu a senha.
   *
   * <p>Existe para quebrar a circularidade do bootstrap no cliente: sem isto, aplicações web
   * precisavam saber o tenant <b>antes</b> de logar, o que na prática virava uma variável embutida
   * no bundle em tempo de build (e portanto um tenant fixo por build) ou uma consulta anônima de
   * descoberta por e-mail. Agora o servidor informa, e o cliente só precisa do e-mail e da senha.
   *
   * <p>É o mesmo valor do claim {@code tenantId} do JWT, que continua sendo a fonte de verdade
   * inforjável; este campo apenas o torna legível sem decodificar o token. {@code nome} e
   * {@code descricao} só vêm preenchidos quando a aplicação registra um
   * {@link ArchbaseTenantInfoResolver}. Nulo quando a linha do usuário não tem tenant.
   */
  @JsonProperty("tenant")
  private TenantLoginOption tenant;

  /** Sinaliza que a senha conferiu mas falta o 2º fator (MFA); os tokens ainda não vêm. */
  @JsonProperty("mfa_required")
  private Boolean mfaRequired;
  /** Token curto de desafio para completar o MFA em {@code POST /auth/mfa/verify}. */
  @JsonProperty("challenge_token")
  private String challengeToken;

}
