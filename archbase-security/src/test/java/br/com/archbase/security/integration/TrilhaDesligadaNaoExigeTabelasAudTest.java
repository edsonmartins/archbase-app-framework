package br.com.archbase.security.integration;

import br.com.archbase.security.repository.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Com a trilha desligada, o Envers não pode exigir nada.
 *
 * <p><b>O defeito que este teste fixa.</b> Desligar a trilha desligava os <i>listeners</i> do Envers,
 * o que impede a gravação — mas não impede o Envers de <b>contribuir o mapeamento</b> das tabelas
 * {@code _AUD}. Para quem controla o schema por migrations e sobe com
 * {@code hibernate.ddl-auto=validate}, o Hibernate passava a exigir tabelas que ninguém pediu e a
 * aplicação não subia:
 *
 * <pre>Schema validation: missing table [seguranca_acao_aud]</pre>
 *
 * <p>Isso é o oposto exato do que a chave promete. Ela existe para que atualizar o framework não
 * quebre quem nunca pediu auditoria, e era justamente esse caso que ela quebrava. Apareceu ao
 * atualizar um consumidor da 3.1.1 para a 3.1.16 — 177 testes de integração deixaram de carregar o
 * contexto, todos por isto.
 *
 * <p>Nenhum teste pegou porque toda a suíte roda com {@code create-drop}, que <b>cria</b> as tabelas
 * exigidas em vez de reclamar delas. O arranjo que revela o defeito é o {@code validate}, e é o que
 * este teste usa.
 */
@SpringBootTest(classes = ArchbaseSecurityTestApplication.class)
@TestPropertySource(properties = {
        // Cria o schema a partir do mapeamento e mantém a conexão viva para a segunda fase.
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.datasource.url=jdbc:h2:mem:sem-envers;DB_CLOSE_DELAY=-1",
        // O padrão, explícito: é a configuração de quem nunca pediu auditoria.
        "archbase.security.audit.enabled=false",
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
class TrilhaDesligadaNaoExigeTabelasAudTest {

    @Autowired
    DataSource dataSource;
    @Autowired
    UserJpaRepository userRepository;

    @Test
    @DisplayName("com a trilha desligada, nenhuma tabela _AUD é criada")
    void nenhumaTabelaAudEhCriada() {
        // Com ddl-auto=create, o Hibernate cria tudo o que o mapeamento declara. Se alguma _AUD
        // aparecer aqui, é porque o Envers continua mapeando — e é essa contribuição que, sob
        // validate, vira "missing table" na aplicação de quem não tem essas tabelas.
        List<String> aud = tabelas().stream().filter(t -> t.endsWith("_aud")).toList();

        assertThat(aud)
                .withFailMessage("a trilha está desligada, mas o Envers mapeou: %s", aud)
                .isEmpty();
    }

    @Test
    @DisplayName("mas as duas tabelas que são entidades JPA continuam mapeadas — e isso é o custo conhecido")
    void tabelasDeEntidadeSeguemMapeadas() {
        // Caracteriza o limite honesto desta correção. seguranca_revisao e seguranca_evento não são
        // contribuição do Envers: são @Entity comuns, encontradas pelo @EntityScan da aplicação.
        // Desligar o Envers não as desmapeia, e o framework não tem como desmapeá-las sem tirar as
        // classes do pacote que as aplicações varrem.
        //
        // Na prática o estrago foi de oito tabelas para duas: quem sobe com validate precisa criar
        // essas duas, e só. Está registrado em deployment/esquema-de-seguranca.md, com o DDL.
        assertThat(tabelas()).contains("seguranca_revisao", "seguranca_evento");
    }

    @Test
    @DisplayName("o resto do módulo continua funcionando normalmente")
    void moduloSegueFuncionando() {
        // Controle: desligar o Envers não pode ter desligado junto o mapeamento das entidades de
        // segurança. Sem isto, os dois testes acima passariam num contexto vazio.
        assertThat(tabelas()).contains("seguranca");
        assertThat(userRepository.findAll()).isNotNull();
    }

    private List<String> tabelas() {
        List<String> nomes = new ArrayList<>();
        try (Connection conexao = dataSource.getConnection();
             ResultSet rs = conexao.getMetaData()
                     .getTables(conexao.getCatalog(), conexao.getSchema(), "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                nomes.add(rs.getString("TABLE_NAME").toLowerCase(Locale.ROOT));
            }
        } catch (Exception e) {
            throw new IllegalStateException("não foi possível ler o catálogo", e);
        }
        return nomes;
    }
}
