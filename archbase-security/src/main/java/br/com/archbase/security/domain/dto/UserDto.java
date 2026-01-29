package br.com.archbase.security.domain.dto;

import br.com.archbase.security.domain.entity.User;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.UUIDGenerator.class, property = "@id")
@JsonIgnoreProperties(value = {"password"}, allowGetters = false, allowSetters = true)
public class UserDto extends SecurityDto {

	protected String userName;
	protected String password;
	protected Boolean changePasswordOnNextLogin;
	protected Boolean allowPasswordChange;
	protected Boolean allowMultipleLogins;
	protected Boolean passwordNeverExpires;
	protected Boolean accountDeactivated = Boolean.FALSE;
	protected Boolean accountLocked = Boolean.FALSE;
	protected Boolean unlimitedAccessHours;
	protected Boolean isAdministrator;
	protected AccessScheduleDto accessSchedule;
	protected List<UserGroupDto> groups = new ArrayList<>();
	protected ProfileDto profile;
	protected byte[] avatar;
	protected String email;
	protected String nickname;
	protected String externalId;

	@Builder
	public UserDto(String id, String code, Long version, LocalDateTime createEntityDate, LocalDateTime updateEntityDate, String createdByUser, String lastModifiedByUser, String name, String description, Set<ActionDto> actions, String userName, String password, Boolean changePasswordOnNextLogin, Boolean allowPasswordChange, Boolean allowMultipleLogins, Boolean passwordNeverExpires, Boolean accountDeactivated, Boolean accountLocked, Boolean unlimitedAccessHours, Boolean isAdministrator, AccessScheduleDto accessSchedule, List<UserGroupDto> groups, ProfileDto profile, byte[] avatar, String email, String nickname, String externalId) {
		super(id, code, version, createEntityDate, updateEntityDate, createdByUser, lastModifiedByUser, name, description, actions);
		this.userName = userName;
		this.password = password;
		this.changePasswordOnNextLogin = changePasswordOnNextLogin;
		this.allowPasswordChange = allowPasswordChange;
		this.allowMultipleLogins = allowMultipleLogins;
		this.passwordNeverExpires = passwordNeverExpires;
		this.accountDeactivated = accountDeactivated;
		this.accountLocked = accountLocked;
		this.unlimitedAccessHours = unlimitedAccessHours;
		this.isAdministrator = isAdministrator;
		this.accessSchedule = accessSchedule;
		this.groups = groups;
		this.profile = profile;
		this.avatar = avatar;
		this.email = email;
		this.nickname = nickname;
		this.externalId = externalId;
	}

	public static UserDto fromDomain(User user) {
		if (user == null) {
			return null;
		}

		return UserDto.builder()
				.id(user.getId().toString())
				.code(user.getCode())
				.version(user.getVersion())
				.createEntityDate(user.getCreateEntityDate())
				.updateEntityDate(user.getUpdateEntityDate())
				.createdByUser(user.getCreatedByUser())
				.lastModifiedByUser(user.getLastModifiedByUser())
				.name(user.getName())
				.description(user.getDescription())
				.email(user.getEmail())
				.userName(user.getUserName())
				.password(user.getPassword())
				.changePasswordOnNextLogin(user.getChangePasswordOnNextLogin())
				.allowPasswordChange(user.getAllowPasswordChange())
				.allowMultipleLogins(user.getAllowMultipleLogins())
				.passwordNeverExpires(user.getPasswordNeverExpires())
				.accountDeactivated(user.getAccountDeactivated())
				.accountLocked(user.getAccountLocked())
				.unlimitedAccessHours(user.getUnlimitedAccessHours())
				.isAdministrator(user.getIsAdministrator())
				.accessSchedule(AccessScheduleDto.fromDomain(user.getAccessSchedule()))
				.groups(user.getGroups() != null?user.getGroups().stream()
						.map(UserGroupDto::fromDomain)
						.collect(Collectors.toList()):null)
				.profile(ProfileDto.fromDomain(user.getProfile()))
				.avatar(user.getAvatar())
				.nickname(user.getNickname())
				.externalId(user.getExternalId())
				.build();
	}

	public User toDomain() {
		return User.builder()
				.id(this.id)
				.code(this.code)
				.version(this.version)
				.createEntityDate(this.createEntityDate)
				.updateEntityDate(this.updateEntityDate)
				.lastModifiedByUser(this.lastModifiedByUser)
				.createdByUser(this.createdByUser)
				.name(this.name)
				.description(this.description)
				.email(this.email)
				.userName(this.userName)
				.password(this.password)
				.changePasswordOnNextLogin(this.changePasswordOnNextLogin)
				.allowPasswordChange(this.allowPasswordChange)
				.allowMultipleLogins(this.allowMultipleLogins)
				.passwordNeverExpires(this.passwordNeverExpires)
				.accountDeactivated(this.accountDeactivated)
				.accountLocked(this.accountLocked)
				.unlimitedAccessHours(this.unlimitedAccessHours)
				.isAdministrator(this.isAdministrator)
				.accessSchedule(this.accessSchedule != null ? this.accessSchedule.toDomain() : null)
				.groups(this.groups != null ? this.groups.stream()
						.map(UserGroupDto::toDomain)
						.collect(Collectors.toList()):null)
				.profile(this.profile != null ? this.profile.toDomain() : null)
				.avatar(this.avatar)
				.nickname(this.nickname)
				.externalId(this.externalId)
				.build();
	}
}
