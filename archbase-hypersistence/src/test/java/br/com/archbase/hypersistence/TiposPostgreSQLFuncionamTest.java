package br.com.archbase.hypersistence;

import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import io.hypersistence.utils.hibernate.type.range.PostgreSQLRangeType;
import io.hypersistence.utils.hibernate.type.range.Range;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Type;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Os tipos PostgreSQL do hypersistence-utils gravam e leem de volta, contra um banco de verdade.
 *
 * <p><b>Por que este teste existe.</b> O módulo archbase-hypersistence não tinha nenhum teste, e o
 * que ele entrega é justamente aquilo que compilação não verifica: os tipos do hypersistence-utils
 * recebem as classes do Hibernate como {@code provided}, ou seja, do runtime. O artefato traz a
 * linha do Hibernate no próprio nome — {@code hypersistence-utils-hibernate-73} — e usar a linha
 * errada atravessa o build inteiro sem sinal, aparecendo só na primeira chamada a um método que
 * mudou de assinatura.
 *
 * <p>Foi o que levou a este teste: o módulo estava no {@code -hibernate-71}, compilado contra
 * Hibernate 7.1/7.2, enquanto o Spring Boot 4.1 entrega 7.4.1. Trocar para o {@code -73} deixava o
 * build verde de qualquer jeito; sem exercitar os tipos contra um banco, "verde" não queria dizer
 * nada.
 *
 * <p><b>O que ele NÃO faz, e convém saber.</b> Rodei estes mesmos casos com o {@code -hibernate-71}
 * de volta, e eles passam igual: nestes caminhos as duas linhas se comportam do mesmo modo. O teste
 * não é, portanto, uma guarda contra usar a linha errada do artefato — ele prova que os tipos que
 * este módulo oferece funcionam de ponta a ponta com o Hibernate e o PostgreSQL que o framework
 * entrega. Alinhar a linha continua certo, mas por higiene, e não por quebra observada.
 *
 * <p><b>O que cobre.</b> Um roundtrip por família de tipo — JSON, array e range —, que são as três
 * que dependem de dialeto e de serialização, e por isso as que quebram quando a linha do Hibernate
 * não bate. Não é teste de funcionalidade do hypersistence-utils, que tem a suíte dele: é teste de
 * que a combinação artefato + Hibernate + PostgreSQL que ESTE framework entrega funciona.
 *
 * <p><b>Testcontainers com {@code disabledWithoutDocker = true}</b>, como no
 * {@code PostgresCompatibilidadeTest} do archbase-security: sem Docker os casos são pulados em vez
 * de quebrarem, para não travar quem constrói o framework numa máquina sem container. O que impede
 * isso de virar cobertura fantasma é o guard do workflow de testes, que trata "pulado" como falha —
 * quem garante a execução é o CI, não a máquina de quem desenvolve.
 */
@Testcontainers(disabledWithoutDocker = true)
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TiposPostgreSQLFuncionamTest.Config.class)
class TiposPostgreSQLFuncionamTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // O schema sai do mapeamento: o que se verifica aqui é o par tipo Java ↔ tipo PostgreSQL,
        // e deixar o Hibernate criar a tabela é o que garante que os dois lados combinam.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = TiposPostgreSQLFuncionamTest.class)
    static class Config {
    }

    @Autowired
    EntityManager em;

    @Test
    @Transactional
    @DisplayName("jsonb: mapa e lista voltam com o mesmo conteúdo")
    void jsonSobreviveAoRoundtrip() {
        Amostra gravada = new Amostra();
        gravada.id = 1L;
        gravada.metadados = Map.of("cor", "azul", "tamanho", "M");
        gravada.etiquetas = List.of("novo", "promoção");

        em.persist(gravada);
        em.flush();
        em.clear();

        Amostra lida = em.find(Amostra.class, 1L);
        assertThat(lida.metadados).containsEntry("cor", "azul").containsEntry("tamanho", "M");
        assertThat(lida.etiquetas).containsExactly("novo", "promoção");
    }

    @Test
    @Transactional
    @DisplayName("text[]: array do PostgreSQL volta na ordem")
    void arrayDeTextoSobreviveAoRoundtrip() {
        Amostra gravada = new Amostra();
        gravada.id = 2L;
        gravada.codigos = new String[]{"A1", "B2", "C3"};

        em.persist(gravada);
        em.flush();
        em.clear();

        Amostra lida = em.find(Amostra.class, 2L);
        assertThat(lida.codigos).containsExactly("A1", "B2", "C3");
    }

    @Test
    @Transactional
    @DisplayName("daterange: intervalo volta com os mesmos limites")
    void rangeDeDatasSobreviveAoRoundtrip() {
        Amostra gravada = new Amostra();
        gravada.id = 3L;
        gravada.vigencia = Range.closed(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        em.persist(gravada);
        em.flush();
        em.clear();

        Amostra lida = em.find(Amostra.class, 3L);
        assertThat(lida.vigencia.lower()).isEqualTo(LocalDate.of(2026, 1, 1));
        // O PostgreSQL normaliza daterange para [inicio,fim): o limite superior fechado vira
        // exclusivo, um dia à frente. Verificar o valor cru aqui é o que documenta esse
        // comportamento para quem for usar o tipo.
        assertThat(lida.vigencia.upper()).isEqualTo(LocalDate.of(2027, 1, 1));
    }

    @Entity
    @Table(name = "amostra_tipos")
    static class Amostra {

        @Id
        Long id;

        @Type(JsonType.class)
        @Column(columnDefinition = "jsonb")
        Map<String, Object> metadados;

        @Type(JsonType.class)
        @Column(columnDefinition = "jsonb")
        List<String> etiquetas;

        @Type(StringArrayType.class)
        @Column(columnDefinition = "text[]")
        String[] codigos;

        @Type(PostgreSQLRangeType.class)
        @Column(columnDefinition = "daterange")
        Range<LocalDate> vigencia;
    }
}
