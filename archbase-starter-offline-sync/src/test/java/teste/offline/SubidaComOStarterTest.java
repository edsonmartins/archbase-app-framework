package teste.offline;

import br.com.archbase.offline.sync.persistence.ProcessedSyncOperationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import teste.offline.app.App;
import teste.offline.app.Pedido;
import teste.offline.app.PedidoRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Adicionar o starter não pode tirar da aplicação as entidades e os repositórios dela. Até a 3.5.0 a auto-configuração
 * declarava @EntityScan e @EnableJpaRepositories: o primeiro faz o Boot parar de procurar entidades no pacote da
 * aplicação, o segundo faz a auto-configuração de repositórios do Boot recuar.
 */
@DisplayName("archbase-starter-offline-sync numa aplicação comum")
class SubidaComOStarterTest {

    @Test
    @DisplayName("a aplicação continua com a sua entidade e o seu repositório, e o starter com o dele")
    void convivem() {
        try (ConfigurableApplicationContext ctx = new SpringApplication(App.class).run(
                "--server.port=0",
                "--spring.datasource.url=jdbc:h2:mem:offline-starter;DB_CLOSE_DELAY=-1",
                "--spring.jpa.hibernate.ddl-auto=create-drop")) {
            PedidoRepository pedidos = ctx.getBean(PedidoRepository.class);
            pedidos.save(new Pedido("p1"));
            assertThat(pedidos.count()).isEqualTo(1);

            ProcessedSyncOperationRepository processadas = ctx.getBean(ProcessedSyncOperationRepository.class);
            assertThat(processadas.count()).isZero();
        }
    }

    @Test
    @DisplayName("aplicação com @EntityScan e @EnableJpaRepositories próprios também recebe a entidade e o repositório do módulo")
    void montagemManual() {
        try (ConfigurableApplicationContext ctx = new SpringApplication(teste.offline.manual.AppManual.class).run(
                "--server.port=0",
                "--spring.datasource.url=jdbc:h2:mem:offline-starter-manual;DB_CLOSE_DELAY=-1",
                "--spring.jpa.hibernate.ddl-auto=create-drop")) {
            var clientes = ctx.getBean(teste.offline.manual.ClienteRepository.class);
            clientes.save(new teste.offline.manual.Cliente("c1"));
            assertThat(clientes.count()).isEqualTo(1);
            assertThat(ctx.getBean(ProcessedSyncOperationRepository.class).count()).isZero();
        }
    }
}
