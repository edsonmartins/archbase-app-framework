package br.com.archbase.security.auth;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
  private byte[] avatar;
  private Boolean changePasswordOnNextLogin;
  private Boolean allowPasswordChange;
  private Boolean allowMultipleLogins;
  private Boolean passwordNeverExpires;
  private Boolean accountDeactivated;
  private Boolean accountLocked;
  private Boolean unlimitedAccessHours;
  private Boolean isAdministrator;
  private String profileName;
  private List<String> groupNames;
  /** Dados da aplicação repassados ao {@code AuthenticationBusinessDelegate.onUserRegistered}. @since 3.5.0 */
  private java.util.Map<String, Object> additionalData;

  /** O construtor com todos os campos de antes da 3.5.0, mantido para quem o chama diretamente. */
  public RegisterNewUser(String name, String description, String userName, String email, String password,
                         byte[] avatar, Boolean changePasswordOnNextLogin, Boolean allowPasswordChange,
                         Boolean allowMultipleLogins, Boolean passwordNeverExpires, Boolean accountDeactivated,
                         Boolean accountLocked, Boolean unlimitedAccessHours, Boolean isAdministrator,
                         String profileName, List<String> groupNames) {
    this(name, description, userName, email, password, avatar, changePasswordOnNextLogin, allowPasswordChange,
            allowMultipleLogins, passwordNeverExpires, accountDeactivated, accountLocked, unlimitedAccessHours,
            isAdministrator, profileName, groupNames, null);
  }
}
