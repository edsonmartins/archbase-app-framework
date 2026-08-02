package br.com.archbase.offline.sync.integration;

import org.junit.jupiter.api.DisplayName;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@DisplayName("DDL entregue — MySQL")
class DdlEntregueMySqlTest extends DdlEntregueBaseTest {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @Override
    protected String jdbcUrl() {
        return MYSQL.getJdbcUrl();
    }

    @Override
    protected String usuario() {
        return MYSQL.getUsername();
    }

    @Override
    protected String senha() {
        return MYSQL.getPassword();
    }
}
