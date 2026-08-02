package br.com.archbase.security.integration;

import org.junit.jupiter.api.DisplayName;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Roda os cenários de {@link BancoCompatibilidadeBaseTest} contra um PostgreSQL de verdade.
 *
 * <p>{@code disabledWithoutDocker = true}: sem Docker os testes são <b>pulados</b>, não quebrados —
 * a suíte precisa continuar rodando numa máquina ou num CI sem container disponível.
 */
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Compatibilidade com PostgreSQL")
class PostgresCompatibilidadeTest extends BancoCompatibilidadeBaseTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
