package br.com.archbase.query.rsql.jpa;

import br.com.archbase.query.rsql.common.RSQLCommonSupport;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** {@code toSort}: formato do javadoc, direção opcional e lista negra. */
@SpringBootTest(classes = OrdenacaoTest.App.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:rsql-ordenacao;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"})
@DisplayName("RSQL: ordenação")
class OrdenacaoTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class App {
    }

    @Entity(name = "LojaOrd")
    public static class Loja {
        @Id
        public String id;
        public String nome;
        public String segredo;

        public Loja() {
        }

        Loja(String id, String nome, String segredo) {
            this.id = id;
            this.nome = nome;
            this.segredo = segredo;
        }
    }

    @Entity(name = "ItemOrd")
    public static class Item {
        @Id
        public String id;
        public Integer preco;
        @ManyToOne
        public Loja loja;

        public Item() {
        }

        Item(String id, Integer preco, Loja loja) {
            this.id = id;
            this.preco = preco;
            this.loja = loja;
        }
    }

    @Autowired
    EntityManager em;
    @Autowired
    TransactionTemplate tx;

    @BeforeEach
    void preparar() {
        RSQLCommonSupport.clear();
        new ArchbaseRSQLJPASupport(Map.of("em", em));
        tx.executeWithoutResult(s -> {
            if (em.find(Item.class, "i1") == null) {
                Loja a = new Loja("l1", "Alfa", "z");
                Loja b = new Loja("l2", "Beta", "a");
                em.persist(a);
                em.persist(b);
                em.persist(new Item("i1", 30, b));
                em.persist(new Item("i2", 10, a));
                em.persist(new Item("i3", 20, b));
            }
        });
    }

    @AfterEach
    void limpar() {
        RSQLCommonSupport.clear();
    }

    private List<String> ordenados(String sort) {
        return tx.execute(s -> {
            var cb = em.getCriteriaBuilder();
            CriteriaQuery<Item> cq = cb.createQuery(Item.class);
            Root<Item> root = cq.from(Item.class);
            ArchbaseRSQLJPASupport.<Item>toSort(sort).toPredicate(root, cq, cb);
            return em.createQuery(cq).getResultList().stream().map(i -> i.id).toList();
        });
    }

    @Test
    @DisplayName("sem direção ordena ascendente (antes, ArrayIndexOutOfBoundsException)")
    void semDirecao() {
        assertThat(ordenados("preco")).containsExactly("i2", "i3", "i1");
    }

    @Test
    @DisplayName("aceita ':' e ',' como no javadoc, com vários campos e caminho por associação")
    void formatos() {
        assertThat(ordenados("preco:desc")).containsExactly("i1", "i3", "i2");
        assertThat(ordenados("loja.nome,asc;preco,desc")).containsExactly("i2", "i1", "i3");
    }

    @Test
    @DisplayName("ordenar por campo da lista negra é recusado, também atravessando a associação")
    void listaNegra() {
        RSQLCommonSupport.addPropertyBlacklist(Loja.class, "segredo");
        assertThatThrownBy(() -> ordenados("loja.segredo")).hasMessageContaining("lista negra");

        RSQLCommonSupport.addPropertyBlacklist(Item.class, "loja");
        assertThatThrownBy(() -> ordenados("loja.nome")).hasMessageContaining("lista negra");
        assertThat(ordenados("preco")).containsExactly("i2", "i3", "i1");
    }
}
