package br.com.archbase.starter.security.auto.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Segurança desligada: avisa uma vez. O Spring Security continua no classpath, e sem a cadeia do Archbase vale a
 * padrão do Spring Boot, que exige autenticação em tudo. Até a 3.5.0 a propriedade era ignorada — quem a tinha em
 * {@code false} e contava com a segurança do Archbase passa a receber 401 em todas as rotas.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "archbase.security", name = "enabled", havingValue = "false")
public class ArchbaseSecurityDisabledWarningAutoConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(ArchbaseSecurityDisabledWarningAutoConfiguration.class);

    public ArchbaseSecurityDisabledWarningAutoConfiguration() {
        LOG.warn("archbase.security.enabled=false: filtro JWT, autorização e endpoints de autenticação do Archbase "
                + "não foram registrados. Com o Spring Security no classpath, vale a cadeia padrão do Spring Boot "
                + "(autenticação exigida em todas as rotas). Até a 3.5.0 esta propriedade era ignorada.");
    }
}
