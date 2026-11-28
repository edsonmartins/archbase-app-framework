package teste.offline.manual;

import br.com.archbase.offline.sync.spi.SyncTenantProvider;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Quem declara entidades e repositórios à mão: o Boot usa só o @EntityScan e recua nos repositórios. */
@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackageClasses = Cliente.class)
@EnableJpaRepositories(basePackageClasses = ClienteRepository.class)
public class AppManual {
    @Bean
    SyncTenantProvider syncTenantProvider() {
        return () -> "t1";
    }
}
