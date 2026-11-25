package teste.starter;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.security.annotation.HasPermission;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.UserJpaRepository;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A negação de {@code @HasPermission} lança {@code AuthorizationDeniedException}, subclasse de
 * {@code AccessDeniedException}. O tratamento de erros comparava a classe exata e respondia 500.
 */
@DisplayName("Negação de @HasPermission na API")
class NegacaoDePermissaoNaApiTest {

    private static final String EMAIL = "sem.permissao@teste.com";
    private static final String SENHA = "Senha-de-teste-1";

    @RestController
    public static class RelatorioController {
        @GetMapping("/teste/relatorio")
        @HasPermission(action = "VIEW", resource = "RELATORIO", description = "Ver relatório")
        public String ver() {
            return "conteúdo";
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class App {
        @Bean
        RelatorioController relatorioController() {
            return new RelatorioController();
        }
    }

    @Test
    @DisplayName("usuário autenticado sem a permissão: 403 ACCESS_DENIED, não 500")
    void autenticadoSemPermissao() throws Exception {
        try (ConfigurableApplicationContext ctx = SubidaDoStarterCompletoTest.subir(App.class,
                "--archbase.app.tenant.default.id=tenant-teste")) {
            criarUsuarioComum(ctx);
            var mvc = SubidaDoStarterCompletoTest.mockMvc(ctx);
            String token = login(ctx, mvc);

            mvc.perform(get("/teste/relatorio").header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }
    }

    @Test
    @DisplayName("rota pública com @HasPermission, sem credencial: 403, não 500")
    void anonimoEmRotaPublica() throws Exception {
        try (ConfigurableApplicationContext ctx = SubidaDoStarterCompletoTest.subir(App.class,
                "--archbase.security.whitelist=/teste/**")) {
            SubidaDoStarterCompletoTest.mockMvc(ctx).perform(get("/teste/relatorio"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }
    }

    private static void criarUsuarioComum(ConfigurableApplicationContext ctx) {
        ArchbaseTenantContext.setTenantId("tenant-teste");
        try {
            UserEntity user = UserEntity.builder()
                    .id("usuario-sem-permissao")
                    .createEntityDate(LocalDateTime.now())
                    .name("Sem Permissão").description("Sem Permissão")
                    .email(EMAIL).userName(EMAIL)
                    .password(ctx.getBean(PasswordEncoder.class).encode(SENHA))
                    .isAdministrator(false)
                    .accountDeactivated(false).accountLocked(false)
                    .changePasswordOnNextLogin(false).passwordNeverExpires(true)
                    .allowPasswordChange(true).allowMultipleLogins(true).unlimitedAccessHours(true)
                    .build();
            user.setTenantId("tenant-teste");
            ctx.getBean(UserJpaRepository.class).save(user);
        } finally {
            ArchbaseTenantContext.clear();
        }
    }

    private static String login(ConfigurableApplicationContext ctx, MockMvc mvc) throws Exception {
        var resposta = mvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"password\":\"" + SENHA + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return new ObjectMapper().readTree(resposta.getResponse().getContentAsString()).get("access_token").asText();
    }
}
