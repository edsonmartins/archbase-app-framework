package br.com.archbase.security.integration;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.security.auth.ArchbaseAuthenticationController;
import br.com.archbase.security.auth.AuthenticationBusinessDelegate;
import br.com.archbase.security.auth.AuthenticationResponse;
import br.com.archbase.security.domain.entity.User;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.AccessTokenJpaRepository;
import br.com.archbase.security.repository.UserJpaRepository;
import br.com.archbase.security.service.ArchbaseAuthenticationService;
import br.com.archbase.security.spi.ArchbaseSocialTokenValidator;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Seis defeitos dos endpoints de login, achados na revisão da documentação (3.5.0):
 * <ol>
 * <li>{@code /register} chamava {@code onUserRegistered} duas vezes;</li>
 * <li>o delegate padrão lançava em {@code onSocialLogin}: o primeiro login social dava 400 com o usuário criado;</li>
 * <li>{@code /login-social} não passava pelo MFA;</li>
 * <li>{@code /login-flexible} pós-processava o desafio de MFA como se fosse login;</li>
 * <li>{@code /resetPassword} dizia "não encontrado" com {@code prevent-user-enumeration=true};</li>
 * <li>"Selecione o tenant" virava 500 em {@code /login} e {@code /authenticate}.</li>
 * </ol>
 * Mesma configuração de {@link FiltroDeAutenticacaoComportamentoAtualTest}, para reaproveitar o contexto em cache.
 */
@SpringBootTest(classes = ArchbaseSecurityTestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:archbase-filtro-fase0-it;DB_CLOSE_DELAY=-1",
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
@DisplayName("Defeitos dos endpoints de login")
class DefeitosDosEndpointsDeLoginTest {

    private static final String TENANT = ArchbaseSecurityTestApplication.TENANT;
    private static final String OUTRO_TENANT = "outro-tenant-login";
    private static final String SENHA = "senha-de-teste";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserJpaRepository userRepository;
    @Autowired
    AccessTokenJpaRepository tokenRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    ArchbaseAuthenticationController controller;
    @Autowired
    ArchbaseAuthenticationService service;

    private final ObjectMapper json = new ObjectMapper();
    private Object delegateOriginalController;
    private Object delegateOriginalServico;
    private Object validadoresOriginais;

    @BeforeEach
    void preparar() {
        limparTenant(TENANT);
        limparTenant(OUTRO_TENANT);
        ArchbaseTenantContext.clear();
        delegateOriginalController = ReflectionTestUtils.getField(alvo(controller), "businessDelegate");
        delegateOriginalServico = ReflectionTestUtils.getField(alvo(service), "businessDelegate");
        validadoresOriginais = ReflectionTestUtils.getField(alvo(controller), "socialTokenValidators");
    }

    @AfterEach
    void restaurar() {
        ReflectionTestUtils.setField(alvo(controller), "businessDelegate", delegateOriginalController);
        ReflectionTestUtils.setField(alvo(service), "businessDelegate", delegateOriginalServico);
        ReflectionTestUtils.setField(alvo(controller), "socialTokenValidators", validadoresOriginais);
        ReflectionTestUtils.setField(alvo(service), "preventUserEnumeration", false);
        SecurityContextHolder.clearContext();
        ArchbaseTenantContext.clear();
    }

    // 1 ─────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("/register chama onUserRegistered uma vez, com os dados do cadastro e o additionalData")
    void registerChamaODelegateUmaVez() throws Exception {
        Delegate delegate = new Delegate();
        usarDelegate(delegate);

        MvcResult r = post("/api/v1/auth/register", Map.of(
                "name", "Nova Pessoa",
                "email", "nova@registro.com.br",
                "password", SENHA,
                "additionalData", Map.of("loja", "L-1", "email", "forjado@x.com")));

        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(delegate.registros).hasSize(1);
        Map<String, Object> dados = delegate.registros.get(0);
        assertThat(dados).containsEntry("loja", "L-1");
        assertThat(dados).as("o campo do cadastro prevalece sobre o additionalData")
                .containsEntry("email", "nova@registro.com.br");
        assertThat(corpo(r).get("businessId").asText()).isEqualTo("negocio-1");
    }

    // 2 ─────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login social de e-mail novo com o delegate padrão entra (era sempre 400)")
    void loginSocialComDelegatePadrao() throws Exception {
        validadorSocial("social-novo@x.com");
        // delegate padrão do contexto, sem sobrescrever onSocialLogin

        MvcResult r = post("/api/v1/auth/login-social", Map.of("provider", "teste", "token", "t"));

        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(r).get("access_token").asText()).isNotBlank();
    }

    // 3 ─────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login social de quem tem MFA devolve o desafio, sem tokens")
    void loginSocialRespeitaMfa() throws Exception {
        usuario(TENANT, "mfa-social@x.com", true);
        validadorSocial("mfa-social@x.com");

        MvcResult r = post("/api/v1/auth/login-social", Map.of("provider", "teste", "token", "t"));

        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        JsonNode corpo = corpo(r);
        assertThat(corpo.get("mfa_required").asBoolean()).isTrue();
        assertThat(corpo.get("challenge_token").asText()).isNotBlank();
        assertThat(corpo.path("access_token").isMissingNode() || corpo.path("access_token").isNull()).isTrue();
    }

    // 4 ─────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("/login-flexible com MFA devolve o desafio sem chamar postAuthenticate nem enrich")
    void loginFlexivelRespeitaMfa() throws Exception {
        usuario(TENANT, "mfa-flex@x.com", true);
        Delegate delegate = new Delegate();
        usarDelegate(delegate);

        MvcResult r = post("/api/v1/auth/login-flexible", Map.of("identifier", "mfa-flex@x.com", "password", SENHA));

        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(r).get("mfa_required").asBoolean()).isTrue();
        assertThat(delegate.posAutenticacoes).isZero();
        assertThat(delegate.enriquecimentos).isZero();
    }

    // 5 ─────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("/resetPassword com prevent-user-enumeration: e-mail inexistente responde como token errado")
    void resetSemEnumeracao() throws Exception {
        usuario(TENANT, "existe@reset.com", false);
        ReflectionTestUtils.setField(alvo(service), "preventUserEnumeration", true);

        MvcResult inexistente = post("/api/v1/auth/resetPassword", Map.of(
                "email", "nao-existe@reset.com", "passwordResetToken", "12345678", "newPassword", "Nova-senha-1"));
        MvcResult tokenErrado = post("/api/v1/auth/resetPassword", Map.of(
                "email", "existe@reset.com", "passwordResetToken", "12345678", "newPassword", "Nova-senha-1"));

        assertThat(inexistente.getResponse().getStatus()).isEqualTo(tokenErrado.getResponse().getStatus());
        assertThat(inexistente.getResponse().getContentAsString())
                .isEqualTo(tokenErrado.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("/resetPassword sem a proteção continua dizendo 'não encontrado' (comportamento de antes)")
    void resetSemProtecaoMantem() throws Exception {
        MvcResult r = post("/api/v1/auth/resetPassword", Map.of(
                "email", "nao-existe@reset.com", "passwordResetToken", "12345678", "newPassword", "Nova-senha-1"));
        assertThat(r.getResponse().getStatus()).isEqualTo(400);
        assertThat(r.getResponse().getContentAsString()).contains("não foi encontrado");
    }

    // 6 ─────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("e-mail em dois tenants: /login, /authenticate e /login-flexible respondem 400 TENANT_SELECTION_REQUIRED")
    void selecaoDeTenant() throws Exception {
        usuario(TENANT, "dois@tenants.com", false);
        usuario(OUTRO_TENANT, "dois@tenants.com", false);
        Map<String, Object> credenciais = Map.of("email", "dois@tenants.com", "password", SENHA);

        for (String rota : List.of("/api/v1/auth/login", "/api/v1/auth/authenticate")) {
            MvcResult r = post(rota, credenciais);
            assertThat(r.getResponse().getStatus()).as(rota).isEqualTo(400);
            assertThat(corpo(r).get("error").asText()).as(rota).isEqualTo("TENANT_SELECTION_REQUIRED");
        }
        MvcResult flex = post("/api/v1/auth/login-flexible",
                Map.of("identifier", "dois@tenants.com", "password", SENHA));
        assertThat(flex.getResponse().getStatus()).isEqualTo(400);
        assertThat(corpo(flex).get("error").asText()).isEqualTo("TENANT_SELECTION_REQUIRED");
    }

    @Test
    @DisplayName("e-mail em dois tenants com tenantId no /authenticate entra")
    void selecaoDeTenantComTenantId() throws Exception {
        usuario(TENANT, "dois2@tenants.com", false);
        usuario(OUTRO_TENANT, "dois2@tenants.com", false);

        MvcResult r = post("/api/v1/auth/authenticate",
                Map.of("email", "dois2@tenants.com", "password", SENHA, "tenantId", OUTRO_TENANT));

        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(r).get("access_token").asText()).isNotBlank();
    }

    @Test
    @DisplayName("e-mail em dois tenants com tenantId no /login e no /login-flexible entra no tenant escolhido")
    void selecaoDeTenantComTenantIdNasOutrasRotas() throws Exception {
        // Até a 3.5.0 os dois DTOs não tinham tenantId (e o /login o descartaria no toBasicRequest): quem tem o
        // e-mail em dois tenants recebia TENANT_SELECTION_REQUIRED e não tinha como responder por essas rotas.
        usuario(TENANT, "dois3@tenants.com", false);
        usuario(OUTRO_TENANT, "dois3@tenants.com", false);

        MvcResult login = post("/api/v1/auth/login",
                Map.of("email", "dois3@tenants.com", "password", SENHA, "tenantId", OUTRO_TENANT));
        assertThat(login.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(login).get("access_token").asText()).isNotBlank();
        assertThat(corpo(login).get("tenant").get("tenantId").asText()).isEqualTo(OUTRO_TENANT);

        MvcResult flex = post("/api/v1/auth/login-flexible",
                Map.of("identifier", "dois3@tenants.com", "password", SENHA, "tenantId", TENANT));
        assertThat(flex.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(flex).get("tenant").get("tenantId").asText()).isEqualTo(TENANT);
    }

    @Test
    @DisplayName("fora do tenant padrão, sem X-TENANT-ID: o login entra no tenant escolhido ou descoberto")
    void loginForaDoTenantPadrao() throws Exception {
        // Senhas diferentes por tenant: com a mesma senha, entrar no tenant errado passaria despercebido — foi assim
        // que o defeito sobreviveu ao teste do tenantId no /authenticate.
        // Até a 3.5.0 a sessão do Hibernate abria na entrada do método @Transactional, com o tenant padrão, e o
        // tenant escolhido ou descoberto não valia: "Login ou senha inválido".
        usuario(TENANT, "dois-senhas@tenants.com", false, "senha-do-padrao");
        usuario(OUTRO_TENANT, "dois-senhas@tenants.com", false, "senha-do-outro");
        usuario(OUTRO_TENANT, "so-no-outro@tenants.com", false, "senha-do-outro");

        for (String rota : List.of("/api/v1/auth/authenticate", "/api/v1/auth/login")) {
            MvcResult escolhido = post(rota,
                    Map.of("email", "dois-senhas@tenants.com", "password", "senha-do-outro", "tenantId", OUTRO_TENANT));
            assertThat(escolhido.getResponse().getStatus()).as(rota).isEqualTo(200);
            assertThat(corpo(escolhido).get("tenant").get("tenantId").asText()).as(rota).isEqualTo(OUTRO_TENANT);

            MvcResult senhaDoOutroTenant = post(rota,
                    Map.of("email", "dois-senhas@tenants.com", "password", "senha-do-padrao", "tenantId", OUTRO_TENANT));
            assertThat(senhaDoOutroTenant.getResponse().getStatus()).as(rota + ": senha de outro tenant").isEqualTo(401);
        }

        MvcResult flex = post("/api/v1/auth/login-flexible",
                Map.of("identifier", "dois-senhas@tenants.com", "password", "senha-do-outro", "tenantId", OUTRO_TENANT));
        assertThat(flex.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(flex).get("tenant").get("tenantId").asText()).isEqualTo(OUTRO_TENANT);

        MvcResult descoberto = post("/api/v1/auth/authenticate",
                Map.of("email", "so-no-outro@tenants.com", "password", "senha-do-outro"));
        assertThat(descoberto.getResponse().getStatus()).as("tenant descoberto pelo e-mail").isEqualTo(200);
        assertThat(corpo(descoberto).get("tenant").get("tenantId").asText()).isEqualTo(OUTRO_TENANT);
    }

    // ═══════════════════════════ apoio ═══════════════════════════

    private MvcResult post(String rota, Map<String, Object> corpo) throws Exception {
        ArchbaseTenantContext.clear();
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(rota)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(corpo)))
                .andReturn();
    }

    private JsonNode corpo(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString());
    }

    private void usuario(String tenant, String email, boolean mfa) {
        usuario(tenant, email, mfa, SENHA);
    }

    private void usuario(String tenant, String email, boolean mfa, String senha) {
        ArchbaseTenantContext.setTenantId(tenant);
        try {
            UserEntity user = UserEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .createEntityDate(LocalDateTime.now())
                    .name(email)
                    .description(email)
                    .email(email)
                    .userName(email)
                    .password(passwordEncoder.encode(senha))
                    .isAdministrator(false)
                    .accountDeactivated(false)
                    .accountLocked(false)
                    .changePasswordOnNextLogin(false)
                    .passwordNeverExpires(true)
                    .allowPasswordChange(true)
                    .allowMultipleLogins(true)
                    .unlimitedAccessHours(true)
                    .build();
            user.setMfaEnabled(mfa);
            user.setTenantId(tenant);
            userRepository.save(user);
        } finally {
            ArchbaseTenantContext.clear();
        }
    }

    private void limparTenant(String tenant) {
        ArchbaseTenantContext.setTenantId(tenant);
        try {
            tokenRepository.deleteAll();
            userRepository.deleteAll();
        } finally {
            ArchbaseTenantContext.clear();
        }
    }

    private void usarDelegate(AuthenticationBusinessDelegate delegate) {
        ReflectionTestUtils.setField(alvo(controller), "businessDelegate", delegate);
        ReflectionTestUtils.setField(alvo(service), "businessDelegate", delegate);
    }

    private void validadorSocial(String email) {
        ArchbaseSocialTokenValidator validador = new ArchbaseSocialTokenValidator() {
            @Override
            public boolean supports(String provider) {
                return "teste".equals(provider);
            }

            @Override
            public Map<String, Object> validate(String provider, String token) {
                Map<String, Object> dados = new HashMap<>();
                dados.put("email", email);
                dados.put("name", "Pessoa Social");
                dados.put("picture", "https://provedor.example/foto.png");   // URL, não bytes
                return dados;
            }
        };
        ReflectionTestUtils.setField(alvo(controller), "socialTokenValidators", List.of(validador));
    }

    private static Object alvo(Object bean) {
        return AopTestUtils.getUltimateTargetObject(bean);
    }

    /** Conta as chamadas. Só sobrescreve o obrigatório; o resto usa os padrões da interface. */
    static final class Delegate implements AuthenticationBusinessDelegate {
        final List<Map<String, Object>> registros = new ArrayList<>();
        int posAutenticacoes;
        int enriquecimentos;

        @Override
        public String onUserRegistered(User user, Map<String, Object> registrationData) {
            registros.add(registrationData);
            return "negocio-" + registros.size();
        }

        @Override
        public boolean supportsContext(String context) {
            return true;
        }

        @Override
        public List<String> getSupportedContexts() {
            return List.of("DEFAULT");
        }

        @Override
        public void postAuthenticate(User user, String context) {
            posAutenticacoes++;
        }

        @Override
        public AuthenticationResponse enrichAuthenticationResponse(AuthenticationResponse baseResponse, String context,
                                                                   HttpServletRequest request) {
            enriquecimentos++;
            return baseResponse;
        }
    }
}
