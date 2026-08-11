package br.com.archbase.security.integration;

import br.com.archbase.security.schema.ArchbaseSecuritySchemaInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Uma base cujas {@code _AUD} nasceram com o padrão do Envers precisa continuar funcionando.
 *
 * <p><b>O incidente que este teste reproduz.</b> Até a 3.1.16, a rotina de esquema montava o próprio
 * mapeamento sem herdar {@code hibernate.integration.envers.enabled}. Com a trilha <b>desligada</b>,
 * o Envers seguia ativo ali e ela criava as tabelas {@code _AUD} — e, como
 * {@code revision_field_name} só é aplicada no ramo "trilha ligada", elas nasciam com {@code rev} e
 * {@code revtype}, o padrão do Envers, e não com {@code id_revisao}/{@code tp_revisao}.
 *
 * <p>O estrago só aparecia no dia em que alguém ligava a trilha, e aparecia como crash-loop:
 *
 * <pre>
 * ERROR: null value in column "rev" of relation "seguranca_recurso_aud"
 * violates not-null constraint (SQLSTATE 23502)
 * </pre>
 *
 * <p>Confirmado em produção pelo intervalo de builds: a imagem com a 3.1.13 criou as tabelas, a com
 * a 3.1.17 ligou a trilha e colidiu.
 *
 * <p><b>Por que a rotina não resolvia sozinha.</b> Ela é aditiva: via faltar {@code id_revisao},
 * emitia {@code ADD COLUMN} e seguia. O {@code rev} legado — {@code NOT NULL} — ficava órfão, e é a
 * convivência dos dois que quebra. Renomear é migração, não adição.
 *
 * <p><b>O que este teste exige.</b> Que as linhas já gravadas <b>sobrevivam</b>. Descartar e recriar
 * as tabelas passaria em qualquer verificação de "a aplicação sobe" e apagaria exatamente aquilo que
 * uma trilha de auditoria existe para guardar.
 */
@SpringBootTest(classes = ArchbaseSecurityTestApplication.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.datasource.url=jdbc:h2:mem:revisao-legada;DB_CLOSE_DELAY=-1",
        "archbase.security.audit.enabled=true",
        "archbase.security.schema.mode=apply",
        "archbase.security.jwt.secret-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "archbase.security.jwt.token-expiration=3600000",
        "archbase.security.jwt.refresh-expiration=86400000",
        "archbase.security.whitelist=",
        "archbase.security.cors.allowed-origins=*",
        "archbase.security.cors.allowed-methods=*",
        "archbase.security.cors.allowed-headers=*",
        "archbase.security.cors.allow-credentials=false",
        "archbase.app.tenant.default.id=tenant-teste"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MigracaoDasColunasDeRevisaoTest {

    private static final String TABELA = "seguranca_recurso_aud";

    @Autowired
    ArchbaseSecuritySchemaInitializer initializer;
    @Autowired
    DataSource dataSource;

    /** Recria a tabela exatamente como a 3.1.13 a deixava: rev/revtype, NOT NULL, e COM histórico. */
    @BeforeEach
    void prepararTabelaLegada() {
        executar("drop table if exists " + TABELA);
        executar("create table " + TABELA + " ("
                + "rev bigint not null, "
                + "revtype smallint, "
                + "id_recurso varchar(40) not null, "
                + "nome varchar(255), "
                + "primary key (rev, id_recurso))");
        // Duas revisões já gravadas. São elas que não podem sumir.
        executar("insert into " + TABELA + " (rev, revtype, id_recurso, nome) "
                + "values (1, 0, 'recurso-1', 'compras.pedido')");
        executar("insert into " + TABELA + " (rev, revtype, id_recurso, nome) "
                + "values (2, 1, 'recurso-1', 'compras.pedido.v2')");
    }

    @Test
    @DisplayName("as colunas de revisão são renomeadas para o padrão do Archbase")
    void colunasSaoRenomeadas() {
        assertThat(colunasDe(TABELA))
                .as("controle: a tabela precisa começar no formato legado")
                .contains("rev", "revtype")
                .doesNotContain("id_revisao", "tp_revisao");

        initializer.conferirAgora();

        Set<String> colunas = colunasDe(TABELA);
        assertThat(colunas).contains("id_revisao", "tp_revisao");
        assertThat(colunas)
                .as("o rev legado não pode sobreviver: NOT NULL e sempre nulo, ele recusa todo insert")
                .doesNotContain("rev", "revtype");
    }

    @Test
    @DisplayName("as revisões já gravadas são preservadas")
    void historicoEhPreservado() {
        initializer.conferirAgora();

        // O ponto central. Recriar a tabela também deixaria o esquema correto e apagaria o histórico
        // — que é justamente o que uma trilha de auditoria existe para guardar.
        assertThat(contar("select count(*) from " + TABELA)).isEqualTo(2);
        assertThat(contar("select count(*) from " + TABELA + " where id_revisao = 1")).isEqualTo(1);
        assertThat(contar("select count(*) from " + TABELA + " where id_revisao = 2")).isEqualTo(1);
    }

    @Test
    @DisplayName("depois de migrada, uma segunda passada não faz nada")
    void segundaPassadaNaoRepete() {
        initializer.conferirAgora();
        Set<String> depoisDaPrimeira = colunasDe(TABELA);

        initializer.conferirAgora();

        assertThat(colunasDe(TABELA)).isEqualTo(depoisDaPrimeira);
        assertThat(contar("select count(*) from " + TABELA)).isEqualTo(2);
    }

    @Test
    @DisplayName("gravar na tabela migrada funciona — que é o que estava quebrado")
    void gravarFuncionaDepoisDaMigracao() {
        initializer.conferirAgora();

        // Reproduz o insert que o Envers faz e que falhava com
        // "null value in column rev violates not-null constraint".
        executar("insert into " + TABELA + " (id_revisao, tp_revisao, id_recurso, nome) "
                + "values (3, 0, 'recurso-2', 'compras.aprovacao')");

        assertThat(contar("select count(*) from " + TABELA)).isEqualTo(3);
    }

    private void executar(String sql) {
        try (Connection conexao = dataSource.getConnection(); Statement st = conexao.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException(sql, e);
        }
    }

    private int contar(String sql) {
        try (Connection conexao = dataSource.getConnection();
             Statement st = conexao.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : -1;
        } catch (SQLException e) {
            throw new IllegalStateException(sql, e);
        }
    }

    private Set<String> colunasDe(String tabela) {
        Set<String> colunas = new LinkedHashSet<>();
        try (Connection conexao = dataSource.getConnection();
             ResultSet rs = conexao.getMetaData()
                     .getColumns(conexao.getCatalog(), conexao.getSchema(), tabela.toUpperCase(Locale.ROOT), "%")) {
            while (rs.next()) {
                colunas.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("não foi possível ler as colunas de " + tabela, e);
        }
        return colunas;
    }
}
