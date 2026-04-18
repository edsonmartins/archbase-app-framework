package br.com.archbase.security.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SimpleUserDto {
    protected String name;
    protected String nickname;
    protected String description;

    @NotBlank(message = "O email é obrigatório")
    @Email(message = "Email inválido")
    protected String email;
	protected String password;
	protected Boolean changePasswordOnNextLogin;
	protected Boolean allowPasswordChange;
	protected Boolean allowMultipleLogins;
	protected Boolean passwordNeverExpires;
	protected Boolean accountDeactivated = Boolean.FALSE;
	protected Boolean accountLocked = Boolean.FALSE;
	protected Boolean unlimitedAccessHours;
	protected Boolean isAdministrator;
	protected List<String> groups = new ArrayList<>();
	protected String profile;

	@Builder
	public SimpleUserDto(String name, String nickname, String description, String email, String password, Boolean changePasswordOnNextLogin, Boolean allowPasswordChange, Boolean allowMultipleLogins, Boolean passwordNeverExpires, Boolean accountDeactivated, Boolean accountLocked, Boolean unlimitedAccessHours, Boolean isAdministrator, List<String> groups, String profile) {
		this.name = name;
		this.nickname = nickname;
		this.description = description;
		this.email = email;
		this.password = password;
		this.changePasswordOnNextLogin = changePasswordOnNextLogin;
		this.allowPasswordChange = allowPasswordChange;
		this.allowMultipleLogins = allowMultipleLogins;
		this.passwordNeverExpires = passwordNeverExpires;
		this.accountDeactivated = accountDeactivated;
		this.accountLocked = accountLocked;
		this.unlimitedAccessHours = unlimitedAccessHours;
		this.isAdministrator = isAdministrator;
		this.groups = groups;
		this.profile = profile;
	}
}
