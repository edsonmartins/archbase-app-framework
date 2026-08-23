package br.com.archbase.security.integration;

import br.com.archbase.security.schema.ArchbaseSecuritySchemaInitializer;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O caso que motivou a rotina de esquema, do começo ao fim.
 *
 * <p>Ligar {@code archbase.security.audit.enabled=true} sem antes criar as tabelas derrubava o
 * sistema — e a ordem correta ("crie as tabelas, depois vire a chave") só estava escrita num
 * arquivo de documentação que ninguém é obrigado a ler antes de mexer numa configuração.
 *
 * <p>O que precisa valer agora: virar a chave e reiniciar basta. As tabelas da trilha — a de eventos
 * e as {@code _AUD} do Envers, que só existem no mapeamento quando a auditoria está ligada — passam
 * a ser criadas pela própria subida.
 */
@SpringBootTest(classes = ArchbaseSecurityTestApplication.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "archbase.security.audit.enabled=true",
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
class EsquemaDaTrilhaCriadoSozinhoTest {

    @Autowired
    ArchbaseSecuritySchemaInitializer initializer;
    @Autowired
    DataSource dataSource;

    @Test
    @DisplayName("com a trilha ligada, as tabelas dela são criadas sozinhas")
    void tabelasDaTrilhaSaoCriadas() {
        derrubar("seguranca_evento");
        derrubar("seguranca_aud");

        assertThat(existe("seguranca_evento")).as("controle: precisa estar ausente").isFalse();
        assertThat(existe("seguranca_aud")).as("controle: precisa estar ausente").isFalse();

        List<String> aplicados = initializer.conferirAgora();

        assertThat(aplicados).isNotEmpty();
        assertThat(existe("seguranca_evento")).as("tabela de eventos").isTrue();
        // A _AUD é a que prova que as configurações do Envers foram herdadas: sem herdá-las, o
        // mapeamento desta rotina não teria tabela de auditoria alguma, e ela criaria só metade do
        // que a trilha precisa — o pior desfecho, porque pareceria resolvido.
        assertThat(existe("seguranca_aud")).as("tabela de auditoria do Envers").isTrue();
    }

    private void derrubar(String tabela) {
        String real = nomeNoCatalogo(tabela);
        if (real == null) {
            return;
        }
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.execute("DROP TABLE IF EXISTS " + real);
        } catch (SQLException e) {
            throw new IllegalStateException("não foi possível derrubar " + tabela, e);
        }
    }

    private boolean existe(String nome) {
        return nomeNoCatalogo(nome) != null;
    }

    private String nomeNoCatalogo(String nome) {
        try (Connection c = dataSource.getConnection();
             ResultSet rs = c.getMetaData().getTables(c.getCatalog(), c.getSchema(), "%",
                     new String[]{"TABLE"})) {
            while (rs.next()) {
                String encontrado = rs.getString("TABLE_NAME");
                if (encontrado.equalsIgnoreCase(nome)) {
                    return encontrado;
                }
            }
            return null;
        } catch (SQLException e) {
            throw new IllegalStateException("não foi possível ler o catálogo", e);
        }
    }
}
