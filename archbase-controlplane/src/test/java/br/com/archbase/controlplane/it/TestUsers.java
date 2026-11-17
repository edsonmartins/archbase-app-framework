package br.com.archbase.controlplane.it;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.UserJpaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.UUID;

final class TestUsers {

    private TestUsers() {
    }

    static UserEntity create(UserJpaRepository users, PasswordEncoder encoder, String tenant, String email,
                             String password, boolean active) {
        String previous = ArchbaseTenantContext.getTenantId();
        ArchbaseTenantContext.setTenantId(tenant);
        try {
            UserEntity user = UserEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .createEntityDate(LocalDateTime.now())
                    .name(email)
                    .description(email)
                    .email(email)
                    .userName(email)
                    .password(encoder.encode(password))
                    .isAdministrator(false)
                    .accountDeactivated(!active)
                    .accountLocked(false)
                    .changePasswordOnNextLogin(false)
                    .passwordNeverExpires(true)
                    .allowPasswordChange(true)
                    .allowMultipleLogins(true)
                    .unlimitedAccessHours(true)
                    .build();
            user.setTenantId(tenant);
            return users.save(user);
        } finally {
            if (previous == null) {
                ArchbaseTenantContext.clear();
            } else {
                ArchbaseTenantContext.setTenantId(previous);
            }
        }
    }
}
