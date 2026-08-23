package br.com.archbase.security.integration;

import org.junit.jupiter.api.DisplayName;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Roda os cenários de {@link BancoCompatibilidadeBaseTest} contra um MySQL de verdade.
 *
 * <p>É o teste que fecha o ERROR 1093: o {@code UPDATE} em lote do logout, que numa versão anterior
 * derivava o {@code userId} por subconsulta sobre a própria tabela, quebrava só aqui — passava em
 * H2 e em PostgreSQL.
 *
 * <p>Sem Docker, é pulado em vez de falhar.
 */
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Compatibilidade com MySQL")
class MySqlCompatibilidadeTest extends BancoCompatibilidadeBaseTest {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }
}
