package br.com.archbase.security.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Roda os cenários de {@link BancoCompatibilidadeBaseTest} contra um PostgreSQL de verdade.
 *
 * <p>{@code disabledWithoutDocker = true}: sem Docker os testes são <b>pulados</b>, não quebrados —
 * a suíte precisa continuar rodando numa máquina ou num CI sem container disponível.
 */
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Compatibilidade com PostgreSQL")
class PostgresCompatibilidadeTest extends BancoCompatibilidadeBaseTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    /**
     * Aplica a migration que o framework entrega — a mesma que o
     * {@code archbase-starter-flyway} injeta nas locations de <b>todo</b> projeto por padrão.
     *
     * <p>Só aqui, e não na classe base: o script é declaradamente específico de PostgreSQL
     * ({@code add column if not exists}, {@code comment on column}). Este teste garante que ele
     * continua executando no banco para o qual foi escrito — um erro de sintaxe nele impediria a
     * subida de todas as aplicações que usam o starter.
     */
    @Test
    @DisplayName("a migration entregue pelo framework executa no PostgreSQL")
    void migrationEntregueExecuta() throws Exception {
        String script;
        try (var in = getClass().getResourceAsStream("/db/migration/archbase/R__archbase_security_schema.sql")) {
            assertThat(in).as("a migration deve estar no classpath").isNotNull();
            script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        try (Connection conn = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            for (String comando : script.lines()
                    .filter(l -> !l.trim().startsWith("--"))
                    .reduce("", (a, b) -> a + "\n" + b)
                    .split(";")) {
                if (comando.isBlank()) {
                    continue;
                }
                try (Statement st = conn.createStatement()) {
                    st.execute(comando);
                }
            }
        }
        // Chegar aqui sem exceção é a asserção: todo comando do script foi aceito.
    }
}
