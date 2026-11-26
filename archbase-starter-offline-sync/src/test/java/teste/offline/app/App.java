package teste.offline.app;

import br.com.archbase.offline.sync.spi.SyncTenantProvider;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;

/** Aplicação comum: entidade e repositório no próprio pacote, sem @EntityScan nem @EnableJpaRepositories. */
@SpringBootConfiguration
@EnableAutoConfiguration
public class App {
    @Bean
    SyncTenantProvider syncTenantProvider() {
        return () -> "t1";
    }
}
