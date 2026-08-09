package br.com.archbase.security.integration;

import br.com.archbase.security.schema.ArchbaseSecuritySchemaInitializer;
import br.com.archbase.security.schema.ArchbaseSecuritySchemaProperties;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O framework cuida do próprio esquema de segurança.
 *
 * <p><b>O que motivou.</b> Atualizar o Archbase passou a exigir DDL que ninguém avisava. O desfecho
 * era sempre o mesmo: a aplicação subia normalmente e quebrava no primeiro uso — {@code relation
 * "seguranca_evento" does not exist} — porque o código que exige a tabela vem do framework e a
 * tabela, não.
 *
 * <p><b>O caso difícil, e o que de fato precisa ser provado.</b> Os projetos que já rodam já têm as
 * tabelas de segurança. Uma rotina que "cria o esquema" não pode presumir banco vazio, nem exigir que
 * alguém declare de que ponto partir. Por isso o teste central aqui não é o da tabela criada — é o de
 * que, num banco já completo, <b>nada acontece</b>.
 *
 * @see ArchbaseSecuritySchemaInitializer
 */
@SpringBootTest(classes = ArchbaseSecurityTestApplication.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
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
// Derruba tabela de propósito; sem isolar o contexto, as outras suítes herdam o banco mutilado.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EsquemaMantidoPeloFrameworkTest {

    @Autowired
    ArchbaseSecuritySchemaInitializer initializer;
    @Autowired
    ArchbaseSecuritySchemaProperties properties;
    @Autowired
    EntityManager entityManager;
    @Autowired
    TransactionTemplate transacao;
    @Autowired
    DataSource dataSource;

    @Test
    @DisplayName("num banco que já tem tudo, nada é executado")
    void bancoCompletoNaoEhTocado() {
        // A ressalva que define o desenho: em projeto que já roda, esta rotina precisa ser um no-op.
        // Se ela produzisse qualquer comando aqui, produziria a cada subida — e o que ela criasse
        // seria, por definição, duplicata do que já existe.
        assertThat(initializer.conferirAgora())
                .as("esquema já completo não deveria gerar comando algum")
                .isEmpty();
    }

    @Test
    @DisplayName("tabela que falta é criada")
    void tabelaAusenteEhCriada() {
        derrubar("SEGURANCA_EVENTO");
        assertThat(existeTabela("seguranca_evento"))
                .as("controle: a tabela precisa estar mesmo ausente, senão o teste não prova nada")
                .isFalse();

        List<String> aplicados = initializer.conferirAgora();

        assertThat(aplicados).isNotEmpty();
        assertThat(existeTabela("seguranca_evento"))
                .as("a tabela deveria ter voltado")
                .isTrue();
    }

    @Test
    @DisplayName("depois de completar, uma segunda passada não faz nada")
    void segundaPassadaEhVazia() {
        derrubar("SEGURANCA_EVENTO");
        initializer.conferirAgora();

        // Idempotência não como promessa, mas verificada: é o que garante que a rotina possa rodar em
        // toda subida sem acumular efeito.
        assertThat(initializer.conferirAgora()).isEmpty();
    }

    @Test
    @DisplayName("em modo report o log mostra o DDL, mas o banco não é tocado")
    void reportNaoEscreve() {
        derrubar("SEGURANCA_EVENTO");
        properties.setMode(ArchbaseSecuritySchemaProperties.Mode.REPORT);
        try {
            List<String> pendentes = initializer.conferirAgora();

            assertThat(pendentes).as("precisa relatar o que falta").isNotEmpty();
            assertThat(existeTabela("seguranca_evento"))
                    .as("report não pode escrever no banco")
                    .isFalse();
        } finally {
            properties.setMode(ArchbaseSecuritySchemaProperties.Mode.APPLY);
            initializer.conferirAgora();
        }
    }

    @Test
    @DisplayName("nenhuma tabela fora do módulo de segurança entra no alcance da rotina")
    void naoTocaEmTabelaDaAplicacao() {
        transacao.executeWithoutResult(t -> entityManager
                .createNativeQuery("CREATE TABLE IF NOT EXISTS TABELA_DA_APLICACAO (ID VARCHAR(40) PRIMARY KEY)")
                .executeUpdate());
        derrubar("SEGURANCA_EVENTO");

        List<String> aplicados = initializer.conferirAgora();

        // O limite não é uma promessa no javadoc: o metadata só contém as entidades do módulo, então
        // nada mais pode aparecer no DDL. Este teste caracteriza isso.
        assertThat(aplicados)
                .as("nenhum comando pode mencionar tabela que não seja do módulo")
                .noneMatch(c -> c.toLowerCase(Locale.ROOT).contains("tabela_da_aplicacao"));
        assertThat(existeTabela("tabela_da_aplicacao")).isTrue();
    }

    private void derrubar(String tabela) {
        transacao.executeWithoutResult(t ->
                entityManager.createNativeQuery("DROP TABLE IF EXISTS " + tabela).executeUpdate());
    }

    private boolean existeTabela(String nome) {
        try (Connection conexao = dataSource.getConnection()) {
            DatabaseMetaData meta = conexao.getMetaData();
            try (ResultSet rs = meta.getTables(conexao.getCatalog(), conexao.getSchema(), "%",
                    new String[]{"TABLE"})) {
                while (rs.next()) {
                    if (rs.getString("TABLE_NAME").equalsIgnoreCase(nome)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            throw new IllegalStateException("não foi possível ler o catálogo", e);
        }
    }
}
