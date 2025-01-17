package br.com.archbase.security.auth;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterNewUser {

  private String name;
  private String description;
  private String userName;
  private String email;
  private String password;
  private RoleUser role;
  private byte[] avatar;
}
