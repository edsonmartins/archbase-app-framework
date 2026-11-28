package teste.offline.http;

import br.com.archbase.offline.sync.spi.SyncHandlerResult;
import br.com.archbase.offline.sync.spi.SyncOperationHandler;
import br.com.archbase.offline.sync.spi.SyncTenantProvider;
import br.com.archbase.offline.sync.dto.SyncOperationDTO;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;

/** Aplicação web com o starter e um handler que lê o payload (JsonNode do Jackson 2), como uma aplicação real. */
@SpringBootConfiguration
@EnableAutoConfiguration
public class AppHttp {

    @Bean
    SyncTenantProvider syncTenantProvider() {
        return () -> "t1";
    }

    /** Devolve como serverVersion o campo "valor" do payload: prova que o handler recebeu o payload lido. */
    @Bean
    SyncOperationHandler leValor() {
        return new SyncOperationHandler() {
            @Override
            public String type() {
                return "LE_VALOR";
            }

            @Override
            public SyncHandlerResult handle(SyncOperationDTO operation) {
                return SyncHandlerResult.version(operation.payload.get("valor").asLong());
            }
        };
    }
}
