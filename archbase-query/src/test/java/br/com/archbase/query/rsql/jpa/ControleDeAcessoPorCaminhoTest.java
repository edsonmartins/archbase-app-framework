package br.com.archbase.query.rsql.jpa;

import br.com.archbase.query.rsql.common.RSQLCommonSupport;
import br.com.archbase.query.rsql.querydsl.ArchbaseRSQLQueryDslSupport;
import com.querydsl.core.types.dsl.PathBuilder;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Controle de acesso do RSQL ao longo do caminho do seletor. Até a 3.5.0 só o último segmento era conferido: a lista
 * negra de {@code Pedido} com {@code cliente} não impedia {@code cliente.cpf==...}, e um seletor terminado na
 * associação lançava NullPointerException. O QueryDSL não conferia os seletores protegidos.
 */
@SpringBootTest(classes = ControleDeAcessoPorCaminhoTest.App.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:rsql-caminho;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"})
@DisplayName("RSQL: controle de acesso por caminho")
class ControleDeAcessoPorCaminhoTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class App {
    }

    @Entity(name = "ClienteRsql")
    public static class Cliente {
        @Id
        public String id;
        public String nome;
        public String cpf;

        public Cliente() {
        }

        Cliente(String id, String nome, String cpf) {
            this.id = id;
            this.nome = nome;
            this.cpf = cpf;
        }
    }

    @Entity(name = "PedidoRsql")
    public static class Pedido {
        @Id
        public String id;
        public String numero;
        @ManyToOne
        public Cliente cliente;

        public Pedido() {
        }

        Pedido(String id, String numero, Cliente cliente) {
            this.id = id;
            this.numero = numero;
            this.cliente = cliente;
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
            if (em.find(Pedido.class, "p1") == null) {
                Cliente ana = new Cliente("c1", "Ana", "111");
                Cliente bia = new Cliente("c2", "Bia", "222");
                em.persist(ana);
                em.persist(bia);
                em.persist(new Pedido("p1", "001", ana));
                em.persist(new Pedido("p2", "002", bia));
            }
        });
    }

    @AfterEach
    void limpar() {
        RSQLCommonSupport.clear();
    }

    private List<String> pedidos(String rsql) {
        return tx.execute(s -> {
            var cb = em.getCriteriaBuilder();
            CriteriaQuery<Pedido> cq = cb.createQuery(Pedido.class);
            Root<Pedido> root = cq.from(Pedido.class);
            cq.where(ArchbaseRSQLJPASupport.<Pedido>rsql(rsql).toPredicate(root, cq, cb));
            return em.createQuery(cq).getResultList().stream().map(p -> p.id).sorted().toList();
        });
    }

    @Test
    @DisplayName("sem listas: atravessar a associação funciona")
    void semListas() {
        assertThat(pedidos("cliente.cpf==111")).containsExactly("p1");
    }

    @Test
    @DisplayName("lista negra no segmento intermediário bloqueia o caminho inteiro")
    void listaNegraNoSegmento() {
        RSQLCommonSupport.addPropertyBlacklist(Pedido.class, "cliente");

        assertThatThrownBy(() -> pedidos("cliente.cpf==111")).hasMessageContaining("lista negra");
        assertThat(pedidos("numero==002")).containsExactly("p2");
    }

    @Test
    @DisplayName("lista branca no segmento: só com o modo estrito (padrão mantém o comportamento anterior)")
    void listaBrancaNoSegmento() {
        RSQLCommonSupport.addPropertyWhitelist(Pedido.class, "numero");

        assertThat(pedidos("cliente.cpf==111")).as("padrão: a lista de Pedido não governa a associação")
                .containsExactly("p1");

        RSQLCommonSupport.setStrictPathAccessControl(true);
        assertThatThrownBy(() -> pedidos("cliente.cpf==111")).hasMessageContaining("lista de permissões");
        assertThat(pedidos("numero==001")).containsExactly("p1");
    }

    @Test
    @DisplayName("lista da classe associada continua valendo para os campos dela")
    void listaDaAssociada() {
        RSQLCommonSupport.addPropertyBlacklist(Cliente.class, "cpf");
        assertThatThrownBy(() -> pedidos("cliente.cpf==111")).hasMessageContaining("lista negra");
        assertThat(pedidos("cliente.nome==Bia")).containsExactly("p2");
    }

    @Test
    @DisplayName("seletor que termina na associação: erro claro, não NullPointerException")
    void terminaNaAssociacao() {
        assertThatThrownBy(() -> pedidos("cliente==c1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não termina num atributo comparável");
    }

    @Test
    @DisplayName("QueryDSL confere os seletores protegidos sob OR, como o JPA")
    void queryDslProtegido() {
        RSQLCommonSupport.addProtectedSelector(Pedido.class, "cliente.id");
        var raiz = new PathBuilder<>(Pedido.class, "pedido");

        assertThatThrownBy(() -> ArchbaseRSQLQueryDslSupport.toPredicate("cliente.id==c1,numero==002", raiz))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
