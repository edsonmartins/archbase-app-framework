package br.com.archbase.oidc.it;

import br.com.archbase.oidc.user.ExternalIdentityStore;
import br.com.archbase.security.auth.external.ExternalBearerAuthenticator;
import br.com.archbase.security.spi.ArchbaseLocalCredentialPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Premissa P2: o jar no classpath sem nenhum provedor configurado não muda nada — nenhum autenticador, nenhuma
 * política de senha, nenhuma tabela. A aplicação sobe e só registra o aviso.
 */
@SpringBootTest(classes = OidcTestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:archbase-oidc-inerte;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "archbase.security.jwt.secret-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "archbase.security.jwt.token-expiration=3600000",
        "archbase.security.jwt.refresh-expiration=86400000",
        "archbase.security.whitelist=",
        "archbase.security.cors.allowed-origins=*",
        "archbase.security.cors.allowed-methods=*",
        "archbase.security.cors.allowed-headers=*",
        "archbase.security.cors.allow-credentials=false",
        "archbase.app.tenant.default.id=tenant-teste"
})
@DisplayName("Módulo OIDC no classpath sem provedor: inerte")
class OidcInerteSemProvedorTest {

    @Autowired
    ApplicationContext context;
    @Autowired
    DataSource dataSource;

    @Test
    @DisplayName("nenhum bean do módulo e nenhuma tabela")
    void inerte() {
        assertThat(context.getBeansOfType(ExternalBearerAuthenticator.class)).isEmpty();
        assertThat(context.getBeansOfType(ArchbaseLocalCredentialPolicy.class)).isEmpty();
        assertThat(context.getBeansOfType(ExternalIdentityStore.class)).isEmpty();
        assertThat(new ExternalIdentityStore(dataSource).tableExists()).isFalse();
        assertThat(new br.com.archbase.oidc.user.RevokedTokenStore(dataSource, java.time.Clock.systemUTC())
                .tableExists()).isFalse();
        assertThat(new br.com.archbase.oidc.user.ExternalLogoutStore(dataSource, java.time.Clock.systemUTC())
                .tableExists()).isFalse();
        assertThat(context.getBeansOfType(br.com.archbase.oidc.exchange.OidcBackchannelLogoutController.class))
                .isEmpty();
        assertThat(new br.com.archbase.oidc.migration.IdentityMigrationJournal(dataSource).tableExists()).isFalse();
        assertThat(context.getBeansOfType(br.com.archbase.oidc.migration.IdentityMigrationController.class))
                .isEmpty();
    }
}
