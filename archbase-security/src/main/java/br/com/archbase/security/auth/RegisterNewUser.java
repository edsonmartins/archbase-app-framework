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
  private Boolean changePasswordOnNextLogin;
  private Boolean allowPasswordChange;
  private Boolean allowMultipleLogins;
  private Boolean passwordNeverExpires;
  private Boolean accountDeactivated;
  private Boolean accountLocked;
  private Boolean unlimitedAccessHours;
  private Boolean isAdministrator;
}
