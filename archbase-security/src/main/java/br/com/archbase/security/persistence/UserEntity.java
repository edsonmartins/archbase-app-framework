package br.com.archbase.security.persistence;

import br.com.archbase.ddd.domain.aspect.annotations.StorageField;
import br.com.archbase.security.domain.dto.UserDto;
import br.com.archbase.security.domain.dto.UserGroupDto;
import br.com.archbase.security.domain.entity.User;
import br.com.archbase.security.domain.entity.UserGroup;
import br.com.archbase.shared.kernel.converters.BooleanToSNConverter;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@DiscriminatorValue("USUARIO")
@Getter
@Setter
public class UserEntity extends SecurityEntity implements UserDetails {

    @Column(name = "USER_NAME")
    private String userName;

    @Column(name = "SENHA")
    private String password;

    @Column(name = "BO_ALTERAR_SENHA_PROXIMO_LOGIN", length = 1)
    @Convert(converter = BooleanToSNConverter.class)
    private Boolean changePasswordOnNextLogin;

    @Column(name = "BO_PERMITE_ALTERAR_SENHA", length = 1)
    @Convert(converter = BooleanToSNConverter.class)
    private Boolean allowPasswordChange;

    @Column(name = "BO_PERMITE_MULTIPLICOS_LOGINS", length = 1)
    @Convert(converter = BooleanToSNConverter.class)
    private Boolean allowMultipleLogins;

    @Column(name = "BO_SENHA_NUNCA_EXPIRA", length = 1)
    @Convert(converter = BooleanToSNConverter.class)
    private Boolean passwordNeverExpires;

    @Column(name = "BO_CONTA_DESATIVADA", length = 1)
    @Convert(converter = BooleanToSNConverter.class)
    private Boolean accountDeactivated;

    @Column(name = "CONTA_BLOQUEADA", length = 1)
    @Convert(converter = BooleanToSNConverter.class)
    private Boolean accountLocked;

    @Column(name = "BO_HORARIO_LIVRE", length = 1)
    @Convert(converter = BooleanToSNConverter.class)
    private Boolean unlimitedAccessHours;

    @Column(name = "BO_ADMINISTRADOR", length = 1)
    @Convert(converter = BooleanToSNConverter.class)
    private Boolean isAdministrator;

    @ManyToOne
    @JoinColumn(name = "HORARIO_ACESSO_ID")
    private AccessScheduleEntity accessSchedule;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserGroupEntity> groups = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "PERFIL_ID")
    private ProfileEntity profile;

    @Lob
    @StorageField
    @Column(name = "AVATAR")
    private byte[] avatar;

    @Column(name = "EMAIL", nullable = true)
    private String email;

    @Column(name = "APELIDO", nullable = true)
    private String nickname;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AccessTokenEntity> tokens = new ArrayList<>();

    public UserEntity() {
        super();
    }

    @Builder
    public UserEntity(String id, String code, Long version, LocalDateTime createEntityDate, String createdByUser, LocalDateTime updateEntityDate, String lastModifiedByUser, String tenantId, String name, String description, String userName, String password, Boolean changePasswordOnNextLogin, Boolean allowPasswordChange, Boolean allowMultipleLogins, Boolean passwordNeverExpires, Boolean accountDeactivated, Boolean accountLocked, Boolean unlimitedAccessHours, Boolean isAdministrator, AccessScheduleEntity accessSchedule, Set<UserGroupEntity> groups, ProfileEntity profile, byte[] avatar, String email, String nickname, List<AccessTokenEntity> tokens) {
        super(id, code, version, createEntityDate, createdByUser, updateEntityDate, lastModifiedByUser, tenantId, name, description);
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
        this.tokens = tokens;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null; // Implementation depends on the roles and permissions logic
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.userName;
    }

    @Override
    public boolean isAccountNonExpired() {
        return !this.accountDeactivated;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !this.accountLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.passwordNeverExpires;
    }

    @Override
    public boolean isEnabled() {
        return !this.accountDeactivated && !this.accountLocked;
    }

    public User toDomain() {
        List<UserGroup> groupsDomain = this.groups != null ? this.groups.stream()
                .map(UserGroupEntity::toDomain)
                .collect(Collectors.toList()) : null;

        return User.builder()
                .id(this.getId())
                .code(this.getCode())
                .version(this.getVersion())
                .updateEntityDate(this.getUpdateEntityDate())
                .createEntityDate(this.getCreateEntityDate())
                .createdByUser(this.getCreatedByUser())
                .lastModifiedByUser(this.getLastModifiedByUser())
                .name(this.getName())
                .description(this.getDescription())
                .email(this.getEmail())
                .userName(this.getUsername())
                .password(this.getPassword())
                .changePasswordOnNextLogin(this.getChangePasswordOnNextLogin())
                .allowPasswordChange(this.getAllowPasswordChange())
                .allowMultipleLogins(this.getAllowMultipleLogins())
                .passwordNeverExpires(this.getPasswordNeverExpires())
                .accountDeactivated(this.getAccountDeactivated())
                .accountLocked(this.getAccountLocked())
                .unlimitedAccessHours(this.getUnlimitedAccessHours())
                .isAdministrator(this.getIsAdministrator())
                .accessSchedule(this.accessSchedule != null ? this.accessSchedule.toDomain(): null)
                .groups(groupsDomain)
                .profile(this.profile != null ? this.profile.toDomain():null)
                .avatar(this.getAvatar())
                .nickname(this.getNickname())
                .build();
    }

    public static UserEntity fromDomain(User user) {
        if (user == null) {
            return null;
        }

        UserEntity userEntity = UserEntity.builder()
                .id(user.getId().toString())
                .code(user.getCode())
                .version(user.getVersion())
                .updateEntityDate(user.getUpdateEntityDate())
                .createEntityDate(user.getCreateEntityDate())
                .createdByUser(user.getCreatedByUser())
                .lastModifiedByUser(user.getUserName())
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
                .profile(ProfileEntity.fromDomain(user.getProfile()))
                .avatar(user.getAvatar())
                .nickname(user.getNickname())
                .build();

        Set<UserGroupEntity> groups = user.getGroups() != null ? user.getGroups()
                .stream()
                .map(item->UserGroupEntity.fromDomain(item,userEntity))
                .collect(Collectors.toSet()):null;

        userEntity.setGroups(groups);

        return userEntity;
    }

    public UserDto toDto() {
        List<UserGroupDto> groupsDto = this.groups != null ? this.groups.stream()
                .map(UserGroupEntity::toDto)
                .collect(Collectors.toList()) : null;

        return UserDto.builder()
                .id(this.getId())
                .code(this.getCode())
                .version(this.getVersion())
                .updateEntityDate(this.getUpdateEntityDate())
                .createEntityDate(this.getCreateEntityDate())
                .createdByUser(this.getCreatedByUser())
                .lastModifiedByUser(this.getLastModifiedByUser())
                .name(this.getName())
                .description(this.getDescription())
                .email(this.getEmail())
                .userName(this.getUsername())
                .password(this.getPassword())
                .changePasswordOnNextLogin(this.getChangePasswordOnNextLogin())
                .allowPasswordChange(this.getAllowPasswordChange())
                .allowMultipleLogins(this.getAllowMultipleLogins())
                .passwordNeverExpires(this.getPasswordNeverExpires())
                .accountDeactivated(this.getAccountDeactivated())
                .accountLocked(this.getAccountLocked())
                .unlimitedAccessHours(this.getUnlimitedAccessHours())
                .isAdministrator(this.getIsAdministrator())
                .accessSchedule(this.accessSchedule != null ? this.accessSchedule.toDto():null)
                .groups(groupsDto)
                .profile(this.profile != null ? this.profile.toDto(): null)
                .avatar(this.getAvatar())
                .nickname(this.getNickname())
                .build();
    }

}
