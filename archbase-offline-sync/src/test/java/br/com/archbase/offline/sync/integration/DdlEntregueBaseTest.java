package br.com.archbase.offline.sync.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Aplica o DDL que o módulo <b>entrega</b> contra o banco real.
 *
 * <p><b>Por que separado dos demais.</b> Os outros testes criam o esquema por {@code ddl-auto}, que
 * é o Hibernate gerando SQL para o dialeto certo — ou seja, não dizem nada sobre o script que a
 * documentação manda o projeto copiar. Este arquivo é um artefato entregue como qualquer outro, e
 * até aqui ninguém o executava fora do PostgreSQL: usava {@code CREATE INDEX IF NOT EXISTS},
 * cláusula que o MySQL não aceita, e o script quebrava em todo projeto MySQL que o seguisse.
 */
abstract class DdlEntregueBaseTest {

    private static final String SCRIPT = "/db/archbase-offline-sync/V001__processed_sync_operation.sql";

    protected abstract String jdbcUrl();

    protected abstract String usuario();

    protected abstract String senha();

    @Test
    @DisplayName("o script de migração entregue executa neste banco")
    void scriptExecuta() throws Exception {
        List<String> comandos = comandosDoScript();
        assertThat(comandos).as("o script precisa ter comandos").isNotEmpty();

        try (Connection conn = DriverManager.getConnection(jdbcUrl(), usuario(), senha())) {
            for (String comando : comandos) {
                assertThatCode(() -> {
                    try (Statement st = conn.createStatement()) {
                        st.execute(comando);
                    }
                })
                        .as("comando do script deve executar neste banco:%n%s", comando)
                        .doesNotThrowAnyException();
            }
        }
    }

    @Test
    @DisplayName("a tabela criada pelo script aceita a chave composta e a recusa duplicada")
    void chaveCompostaFunciona() throws Exception {
        try (Connection conn = DriverManager.getConnection(jdbcUrl(), usuario(), senha())) {
            for (String comando : comandosDoScript()) {
                try (Statement st = conn.createStatement()) {
                    st.execute(comando);
                } catch (Exception jaExiste) {
                    // O teste anterior pode ter criado; o que importa é o estado final.
                }
            }

            try (Statement st = conn.createStatement()) {
                st.executeUpdate("DELETE FROM processed_sync_operation");
                st.executeUpdate("INSERT INTO processed_sync_operation "
                        + "(tenant_id, operation_id, status, processed_at) "
                        + "VALUES ('t1', 'op1', 'PROCESSED', CURRENT_TIMESTAMP)");
            }

            boolean duplicataRecusada = false;
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("INSERT INTO processed_sync_operation "
                        + "(tenant_id, operation_id, status, processed_at) "
                        + "VALUES ('t1', 'op1', 'PROCESSED', CURRENT_TIMESTAMP)");
            } catch (Exception esperado) {
                duplicataRecusada = true;
            }

            assertThat(duplicataRecusada)
                    .as("a PK composta (tenant_id, operation_id) é o que garante a idempotência")
                    .isTrue();
        }
    }

    @Test
    @DisplayName("a tabela criada pelo script tem toda coluna que a entidade mapeia")
    void colunasDaEntidadeExistem() throws Exception {
        try (Connection conn = DriverManager.getConnection(jdbcUrl(), usuario(), senha())) {
            for (String comando : comandosDoScript()) {
                try (Statement st = conn.createStatement()) {
                    st.execute(comando);
                } catch (Exception jaExiste) {
                    // O teste anterior pode ter criado; o que importa é o estado final.
                }
            }
            java.util.Set<String> naTabela = new java.util.HashSet<>();
            try (Statement st = conn.createStatement();
                 java.sql.ResultSet rs = st.executeQuery("SELECT * FROM processed_sync_operation WHERE 1 = 0")) {
                java.sql.ResultSetMetaData md = rs.getMetaData();
                for (int i = 1; i <= md.getColumnCount(); i++) {
                    naTabela.add(md.getColumnName(i).toLowerCase(java.util.Locale.ROOT));
                }
            }
            java.util.List<String> mapeadas = Arrays.stream(
                            br.com.archbase.offline.sync.persistence.ProcessedSyncOperation.class.getDeclaredFields())
                    .map(f -> f.getAnnotation(jakarta.persistence.Column.class))
                    .filter(java.util.Objects::nonNull)
                    .map(c -> c.name().toLowerCase(java.util.Locale.ROOT))
                    .toList();
            assertThat(mapeadas).as("a entidade mapeia colunas").isNotEmpty();
            assertThat(naTabela)
                    .as("sem a coluna, o INSERT da entidade falha e ddl-auto=validate não sobe")
                    .containsAll(mapeadas);
        }
    }

    /** Divide o script em comandos, ignorando comentários de linha. */
    private List<String> comandosDoScript() throws IOException {
        try (InputStream in = getClass().getResourceAsStream(SCRIPT)) {
            assertThat(in).as("script %s deve estar no classpath", SCRIPT).isNotNull();
            String conteudo = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            String semComentarios = conteudo.lines()
                    .filter(linha -> !linha.trim().startsWith("--"))
                    .reduce("", (a, b) -> a + "\n" + b);
            return Arrays.stream(semComentarios.split(";"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
    }
}
