package br.com.archbase.oidc.it;

import br.com.archbase.oidc.user.ExternalIdentityStore;
import br.com.archbase.security.auth.external.ExternalBearerAuthenticator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code archbase.security.schema.mode=report}: quem controla o esquema por migration não pode ter uma tabela criada
 * por baixo. O módulo sobe, avisa com o DDL e não cria nada.
 */
@SpringBootTest(classes = OidcTestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:archbase-oidc-report;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "archbase.security.jwt.secret-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "archbase.security.jwt.token-expiration=3600000",
        "archbase.security.jwt.refresh-expiration=86400000",
        "archbase.security.whitelist=",
        "archbase.security.cors.allowed-origins=*",
        "archbase.security.cors.allowed-methods=*",
        "archbase.security.cors.allowed-headers=*",
        "archbase.security.cors.allow-credentials=false",
        "archbase.app.tenant.default.id=tenant-teste",
        "archbase.security.schema.mode=report",
        "archbase.security.oidc.providers.idp.issuer=https://idp.example.com",
        "archbase.security.oidc.providers.idp.audiences[0]=app",
        "archbase.security.oidc.providers.idp.tenant.fixed=tenant-teste"
})
@DisplayName("Esquema em modo report: a tabela de vínculo não é criada")
class OidcEsquemaEmModoReportTest {

    @Autowired
    ApplicationContext context;
    @Autowired
    ExternalIdentityStore store;
    @Autowired
    org.springframework.test.web.servlet.MockMvc mockMvc;

    @Test
    @DisplayName("módulo ativo, tabela ausente")
    void naoCria() {
        assertThat(context.getBeansOfType(ExternalBearerAuthenticator.class)).hasSize(1);
        assertThat(store.tableExists()).isFalse();
        assertThat(context.getBean(br.com.archbase.oidc.user.RevokedTokenStore.class).tableExists()).isFalse();
        assertThat(context.getBean(br.com.archbase.oidc.user.ExternalLogoutStore.class).tableExists()).isFalse();
    }

    @Test
    @DisplayName("troca de token desligada por padrão: o endpoint não existe")
    void trocaDesligada() throws Exception {
        int status = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/oidc/exchange")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"token\":\"x\"}"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isEqualTo(404);
        assertThat(context.getBeansOfType(br.com.archbase.oidc.exchange.OidcTokenExchangeController.class)).isEmpty();
    }
}
