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

    /**
     * A migration sozinha produz as colunas que as entidades exigem.
     *
     * <p><b>Por que não basta o teste acima.</b> Ali o script roda sobre um schema que o Hibernate
     * já criou a partir das entidades, então todo {@code add column if not exists} é no-op: o teste
     * prova que o SQL é <i>válido</i>, não que ele <i>cria</i> alguma coisa. Um erro de digitação
     * entre o nome na entidade e o nome no script passaria despercebido — e é justamente nos
     * projetos com {@code ddl-auto=validate} ou {@code none}, onde a migration é a única fonte do
     * schema, que esse erro impede a aplicação de subir.
     *
     * <p>Aqui o script é aplicado a um schema <b>vazio</b>, com as tabelas criadas apenas com a
     * coluna de chave. O que existir depois veio do script, e de mais nada.
     */
    @Test
    @DisplayName("a migration cria as colunas do core em um schema vazio")
    void migrationCriaAsColunasDoCore() throws Exception {
        String script;
        try (var in = getClass().getResourceAsStream("/db/migration/archbase/R__archbase_security_schema.sql")) {
            script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        try (Connection conn = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {

            try (Statement st = conn.createStatement()) {
                st.execute("drop schema if exists migracao_limpa cascade");
                st.execute("create schema migracao_limpa");
                st.execute("set search_path to migracao_limpa");
                // Só a chave: tudo o mais precisa vir do script.
                st.execute("create table seguranca (id_seguranca varchar(40) primary key, tp_seguranca varchar(50))");
                st.execute("create table seguranca_acao (id_acao varchar(40) primary key)");
                st.execute("create table seguranca_permissao (id_permissao varchar(40) primary key)");
                st.execute("create table seguranca_token_api (id_token_api varchar(40) primary key, token varchar(255) not null)");
                st.execute("create table seguranca_token_acesso (id_token_acesso varchar(40) primary key)");
            }

            for (String comando : script.lines()
                    .filter(l -> !l.trim().startsWith("--"))
                    .reduce("", (a, b) -> a + "\n" + b)
                    .split(";")) {
                if (comando.isBlank()) {
                    continue;
                }
                try (Statement st = conn.createStatement()) {
                    st.execute("set search_path to migracao_limpa");
                    st.execute(comando);
                }
            }

            assertThat(colunaExiste(conn, "seguranca_acao", "minimum_level"))
                    .as("piso da capacidade — ActionEntity.minimumLevel")
                    .isTrue();
            assertThat(colunaExiste(conn, "seguranca", "access_level"))
                    .as("nível do perfil — ProfileEntity.accessLevel")
                    .isTrue();
            assertThat(colunaExiste(conn, "seguranca_permissao", "effect"))
                    .as("GRANT | DENY — PermissionEntity.effect")
                    .isTrue();

            try (Statement st = conn.createStatement()) {
                st.execute("drop schema if exists migracao_limpa cascade");
            }
        }
    }

    private boolean colunaExiste(Connection conn, String tabela, String coluna) throws Exception {
        try (Statement st = conn.createStatement();
             var rs = st.executeQuery(
                     "SELECT COUNT(*) FROM information_schema.columns "
                             + "WHERE table_schema = 'migracao_limpa' "
                             + "AND table_name = '" + tabela + "' AND column_name = '" + coluna + "'")) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }
}
