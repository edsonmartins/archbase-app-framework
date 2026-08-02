package br.com.archbase.offline.sync.integration;

import org.junit.jupiter.api.DisplayName;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@DisplayName("DDL entregue — PostgreSQL")
class DdlEntreguePostgresTest extends DdlEntregueBaseTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Override
    protected String jdbcUrl() {
        return POSTGRES.getJdbcUrl();
    }

    @Override
    protected String usuario() {
        return POSTGRES.getUsername();
    }

    @Override
    protected String senha() {
        return POSTGRES.getPassword();
    }
}
