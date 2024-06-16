package br.com.archbase.security.auth;

import lombok.*;

@Getter
@Setter
public class RefreshTokenRequest {

  private String token;
}
