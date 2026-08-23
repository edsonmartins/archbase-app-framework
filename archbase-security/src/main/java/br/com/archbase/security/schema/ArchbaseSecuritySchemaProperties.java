package br.com.archbase.security.schema;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Como o framework cuida do próprio esquema de segurança.
 *
 * @see ArchbaseSecuritySchemaInitializer
 */
@ConfigurationProperties(prefix = "archbase.security.schema")
public class ArchbaseSecuritySchemaProperties {

    public enum Mode {
        /** Não olha o banco. Para quem controla o esquema por migrations e não quer nem a conferência. */
        OFF,
        /** Confere e escreve no log o DDL que falta, sem executar nada. */
        REPORT,
        /** Confere e cria o que falta. Nunca remove, nunca altera o que já existe. */
        APPLY
    }

    private Mode mode = Mode.APPLY;

    /**
     * Se um erro aqui derruba a aplicação.
     *
     * <p>Falso por padrão, e a razão é a mesma que já custou caro na trilha de auditoria: uma rotina
     * acessória não pode derrubar o que ela serve. Sem permissão de DDL no banco, a aplicação sobe
     * exatamente como subia antes desta rotina existir — o que falta aparece no log, em vez de virar
     * uma aplicação que não sobe.
     *
     * <p>Ligue em homologação para descobrir o problema antes da produção.
     */
    private boolean failOnError = false;

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public boolean isFailOnError() {
        return failOnError;
    }

    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }
}
