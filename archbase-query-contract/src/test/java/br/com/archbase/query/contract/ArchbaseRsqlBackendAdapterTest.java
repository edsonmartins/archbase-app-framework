package br.com.archbase.query.contract;

import br.com.archbase.query.rsql.common.RSQLCommonSupport;
import br.com.archbase.query.rsql.jpa.ArchbaseRSQLJPASupport;
import io.github.ggomarighetti.rsqljpasearch.compile.CompiledSearch;
import io.github.ggomarighetti.rsqljpasearch.compile.SearchCompiler;
import io.github.ggomarighetti.rsqljpasearch.definition.SearchDefinition;
import io.github.ggomarighetti.rsqljpasearch.policy.SearchPolicy;
import io.github.ggomarighetti.rsqljpasearch.rsql.engine.SearchRsqlEngine;
import io.github.ggomarighetti.rsqljpasearch.rsql.engine.SearchRsqlEngines;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de integração ponta a ponta: a camada de contrato da {@code rsql-jpa-search} compila um
 * filtro RSQL através do {@link ArchbaseRsqlBackendAdapter} (motor do Archbase) e executa contra um
 * banco H2 real. Valida filtro por campo simples, por path mapeado (associação) e por operador de
 * comparação. O JPA é inicializado manualmente (sem slices do Spring Boot) para autossuficiência.
 */
class ArchbaseRsqlBackendAdapterTest {

    /** Contrato: expõe "name", "price" e "category" (mapeado para o path interno category.name). */
    private static final SearchDefinition<Product> DEFINITION = SearchDefinition.builder()
            .entity(Product.class)
            .fields(fields -> {
                fields.add("name", String.class, field -> field.filterable());
                fields.add("category", String.class, field -> field.path("category.name").filterable());
                fields.add("price", Integer.class, field -> field.filterable());
            })
            .build();

    private static EntityManagerFactory entityManagerFactory;

    private EntityManager entityManager;
    private SearchCompiler compiler;

    @BeforeAll
    static void bootstrapJpa() {
        entityManagerFactory = Persistence.createEntityManagerFactory("contractPU");
    }

    @AfterAll
    static void shutdownJpa() {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }

    @BeforeEach
    void setUp() {
        entityManager = entityManagerFactory.createEntityManager();

        // Liga o estado estático do motor do Archbase ao EntityManager (o que o
        // ArchbaseRSQLConfiguration faz em runtime).
        RSQLCommonSupport.clear();
        new ArchbaseRSQLJPASupport(Map.of("default", entityManager));

        SearchRsqlEngine engine = SearchRsqlEngines.builder(new ArchbaseRsqlBackendAdapter())
                .conversionService(new DefaultConversionService())
                .build();
        // Política que permite consultas sem paginação, para focar o teste no filtro/adapter.
        SearchPolicy policy = SearchPolicy.defaults().toBuilder()
                .paging(paging -> paging
                        .allowUnpaged(true)
                        .defaultUnpagedSize(100)
                        .maxUnpagedSize(1000))
                .build();
        compiler = new SearchCompiler(engine, policy);

        entityManager.getTransaction().begin();
        Category perifericos = new Category("Perifericos");
        Category monitores = new Category("Monitores");
        entityManager.persist(perifericos);
        entityManager.persist(monitores);
        entityManager.persist(new Product("Teclado", 200, perifericos));
        entityManager.persist(new Product("Mouse", 100, perifericos));
        entityManager.persist(new Product("Monitor 24", 900, monitores));
        entityManager.getTransaction().commit();
    }

    @AfterEach
    void tearDown() {
        if (entityManager != null && entityManager.isOpen()) {
            entityManager.getTransaction().begin();
            entityManager.createQuery("delete from Product").executeUpdate();
            entityManager.createQuery("delete from Category").executeUpdate();
            entityManager.getTransaction().commit();
            entityManager.close();
        }
    }

    /** Compila o filtro pelo pipeline de contrato e executa o Specification resultante via Criteria. */
    private List<Product> search(String filter) {
        CompiledSearch<Product> compiled =
                compiler.compile(filter, null, Pageable.unpaged(), DEFINITION);
        return runQuery(compiled.specification());
    }

    private List<Product> runQuery(Specification<Product> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Product> cq = cb.createQuery(Product.class);
        Root<Product> root = cq.from(Product.class);
        Predicate predicate = specification.toPredicate(root, cq, cb);
        if (predicate != null) {
            cq.where(predicate);
        }
        return entityManager.createQuery(cq).getResultList();
    }

    @Test
    void filtraPorCampoSimples() {
        assertThat(search("name==Teclado"))
                .extracting(Product::getName)
                .containsExactly("Teclado");
    }

    @Test
    void filtraPorPathMapeadoDeAssociacao() {
        // selector público "category" -> path interno "category.name"
        assertThat(search("category==Perifericos"))
                .extracting(Product::getName)
                .containsExactlyInAnyOrder("Teclado", "Mouse");
    }

    @Test
    void filtraComOperadorDeComparacao() {
        assertThat(search("price=gt=150"))
                .extracting(Product::getName)
                .containsExactlyInAnyOrder("Teclado", "Monitor 24");
    }

    @Test
    void filtraComExpressaoCompostaAndOr() {
        // governança + motor: AND/OR compilados ponta a ponta
        assertThat(search("category==Perifericos;price=lt=150"))
                .extracting(Product::getName)
                .containsExactly("Mouse");
    }
}
