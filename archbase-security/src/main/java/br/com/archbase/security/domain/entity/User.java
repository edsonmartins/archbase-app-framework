package br.com.archbase.security.domain.entity;

import br.com.archbase.ddd.domain.annotations.DomainAggregateRoot;
import br.com.archbase.ddd.domain.contracts.AggregateRoot;
import br.com.archbase.ddd.domain.contracts.Identifier;
import br.com.archbase.ddd.domain.contracts.SecurityUser;
import br.com.archbase.ddd.domain.base.ArchbaseIdentifier;
import br.com.archbase.validation.fluentvalidator.AbstractArchbaseValidator;
import br.com.archbase.validation.fluentvalidator.context.ArchbaseValidationResult;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Getter
@DomainAggregateRoot
public class User extends Security<User, User> implements AggregateRoot<User, Identifier>, SecurityUser {

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
    protected AccessSchedule accessSchedule;
    protected List<UserGroup> groups = new ArrayList<>();
    protected Profile profile;
    protected byte[] avatar;


    @Builder
    public User(String id, String code, Long version, LocalDateTime updateEntityDate, LocalDateTime createEntityDate, String createdByUser, String lastModifiedByUser, String name, String description, AccessSchedule accessSchedule, String email, String userName, String password, Boolean changePasswordOnNextLogin, Boolean allowPasswordChange, Boolean allowMultipleLogins, Boolean passwordNeverExpires, Boolean accountDeactivated, Boolean accountLocked, Boolean unlimitedAccessHours, Boolean isAdministrator, AccessSchedule accessSchedule1, List<UserGroup> groups, Profile profile, byte[] avatar) {
        super(id, code, version, updateEntityDate, createEntityDate, createdByUser, lastModifiedByUser, name, description, accessSchedule, email);
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
        this.accessSchedule = accessSchedule1;
        this.groups = groups;
        this.profile = profile;
        this.avatar = avatar;
    }

    static class Validator extends AbstractArchbaseValidator<User> {
        @Override
        public void rules() {

        }
    }

    @Override
    public ArchbaseValidationResult validate() {
        return new Validator().validate(this);
    }
}

