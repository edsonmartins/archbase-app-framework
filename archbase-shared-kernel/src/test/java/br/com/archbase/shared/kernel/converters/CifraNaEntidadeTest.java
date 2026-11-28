package br.com.archbase.shared.kernel.converters;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Com o Spring Boot, o Hibernate cria o conversor pelo contêiner do Spring, e a chave de
 * {@code archbase.shared.crypto.key} chega a ele sem nenhuma configuração extra.
 */
@SpringBootTest(classes = CifraNaEntidadeTest.App.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:cifra-na-entidade;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "archbase.shared.crypto.key=q83vEi7uBQ0hT2bSFZ4LqgnFsx2W0Ckzm1vy3pA9Rjc="})
@DisplayName("Cifra de atributo numa entidade JPA")
class CifraNaEntidadeTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class App {
    }

    @Entity(name = "Paciente")
    public static class Paciente {
        @Id
        public String id;

        @Convert(converter = StringEncryptDecryptConverter.class)
        public String documento;

        public Paciente() {
        }

        Paciente(String id, String documento) {
            this.id = id;
            this.documento = documento;
        }
    }

    @Autowired
    EntityManager entityManager;
    @Autowired
    TransactionTemplate transacao;
    @Autowired
    JdbcTemplate jdbc;

    @Test
    @DisplayName("a coluna guarda o formato atual e a entidade lê o texto")
    void cifraComAChaveConfigurada() {
        transacao.executeWithoutResult(s -> entityManager.persist(new Paciente("p1", "123.456.789-09")));

        String coluna = jdbc.queryForObject("select documento from paciente where id = 'p1'", String.class);
        assertThat(coluna).startsWith("v2:").doesNotContain("123");

        entityManager.clear();
        Paciente lido = transacao.execute(s -> entityManager.find(Paciente.class, "p1"));
        assertThat(lido.documento).isEqualTo("123.456.789-09");
    }
}
