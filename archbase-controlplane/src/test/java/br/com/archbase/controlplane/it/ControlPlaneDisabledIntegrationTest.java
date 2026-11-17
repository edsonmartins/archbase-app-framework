package br.com.archbase.controlplane.it;

import br.com.archbase.controlplane.auth.ControlPlaneBearerAuthenticator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Com o jar no classpath e a chave desligada, nada muda: nem beans, nem o tenant padrão. */
@SpringBootTest(classes = ControlPlaneTestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:cp-off;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "archbase.security.jwt.token-expiration=3600000",
        "archbase.security.jwt.refresh-expiration=86400000",
        "archbase.security.whitelist=/api/publico/**",
        "archbase.security.cors.allowed-origins=*",
        "archbase.security.cors.allowed-methods=*",
        "archbase.security.cors.allowed-headers=*",
        "archbase.security.cors.allow-credentials=false",
        "archbase.app.tenant.default.id=" + ControlPlaneTestApplication.DEFAULT_TENANT
})
@DisplayName("Control Plane desligado")
class ControlPlaneDisabledIntegrationTest {

    @DynamicPropertySource
    static void segredos(DynamicPropertyRegistry registry) {
        registry.add("archbase.security.jwt.secret-key", TestSecrets::legacyJwtKey);
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("nenhum bean do módulo é criado")
    void semBeans() {
        assertThat(context.getBeansOfType(ControlPlaneBearerAuthenticator.class)).isEmpty();
    }

    @Test
    @DisplayName("o tenant padrão continua valendo como antes")
    void tenantPadraoComoAntes() throws Exception {
        mvc.perform(get("/api/publico/tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenant").value(ControlPlaneTestApplication.DEFAULT_TENANT));
    }
}
