package br.com.archbase.security.config;

import br.com.archbase.security.password.ArchbasePasswordPolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Publica a configuração de expiração de senha no {@link ArchbasePasswordPolicy}.
 *
 * <p>A cópia para estado estático acontece na construção do bean, antes de qualquer
 * autenticação ser processada.
 */
@Slf4j
@Configuration
public class ArchbasePasswordPolicyConfiguration {

    public ArchbasePasswordPolicyConfiguration(
            @Value("${archbase.security.password.expiration-days:0}") int expirationDays) {
        ArchbasePasswordPolicy.setExpirationDays(expirationDays);
        if (ArchbasePasswordPolicy.isExpirationEnabled()) {
            log.info("Expiração periódica de senha habilitada: {} dias", ArchbasePasswordPolicy.getExpirationDays());
        } else {
            log.debug("Expiração periódica de senha desabilitada (archbase.security.password.expiration-days=0)");
        }
    }
}
