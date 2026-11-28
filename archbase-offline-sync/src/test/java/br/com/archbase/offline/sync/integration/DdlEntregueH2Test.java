package br.com.archbase.offline.sync.integration;

import org.junit.jupiter.api.DisplayName;

/** O mesmo DDL entregue em H2: roda sempre, mesmo sem Docker para os bancos reais. */
@DisplayName("DDL entregue — H2")
class DdlEntregueH2Test extends DdlEntregueBaseTest {

    @Override
    protected String jdbcUrl() {
        // Banco novo por conexão: cada teste aplica o script desde o início.
        return "jdbc:h2:mem:ddl-" + java.util.UUID.randomUUID();
    }

    @Override
    protected String usuario() {
        return "sa";
    }

    @Override
    protected String senha() {
        return "";
    }
}
