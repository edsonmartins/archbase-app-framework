package br.com.archbase.security.integration;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.security.auth.external.ExternalBearerAuthenticator;
import br.com.archbase.security.auth.external.ExternalTokenCheck;
import br.com.archbase.security.config.ArchbaseJwtAuthenticationFilter;
import br.com.archbase.security.persistence.ApiTokenEntity;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.AccessTokenJpaRepository;
import br.com.archbase.security.repository.ApiTokenRepository;
import br.com.archbase.security.repository.UserJpaRepository;
import br.com.archbase.security.spi.ArchbaseCompanyAccessResolver;
import br.com.archbase.security.util.ApiTokenHasher;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code archbase.security.company.validation}: o {@code X-COMPANY-ID} conferido contra o usuário autenticado, pelo
 * {@link ArchbaseCompanyAccessResolver}, nos três caminhos do filtro (JWT próprio, token de API, token externo).
 * <p>
 * O padrão {@code off} mantém o comportamento de sempre — o cabeçalho vai para o contexto sem conferência; quem prova
 * é {@link FiltroDeAutenticacaoComportamentoAtualTest#companyDoHeader}, inalterado. Mesma configuração daquele
 * teste, para reaproveitar o contexto em cache.
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
@DisplayName("Conferência do X-COMPANY-ID")
class ConferenciaDeEmpresaTest {

    private static final String TENANT = ArchbaseSecurityTestApplication.TENANT;
    private static final String EMAIL = "empresa@conferencia.com.br";
    private static final String SENHA = "senha-de-teste";
    private static final String PERMITIDA = "empresa-a";
    private static final String ALHEIA = "empresa-b";

    @Autowired
    ArchbaseJwtAuthenticationFilter filtro;
    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserJpaRepository userRepository;
    @Autowired
    AccessTokenJpaRepository tokenRepository;
    @Autowired
    ApiTokenRepository apiTokenRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    private final ObjectMapper json = new ObjectMapper();
    private UserEntity usuario;

    /** Só {@link #PERMITIDA}, para o usuário do teste. */
    private final ArchbaseCompanyAccessResolver resolver =
            (user, companyId) -> EMAIL.equals(user.getEmail()) && PERMITIDA.equals(companyId);

    @BeforeEach
    void preparar() {
        apiTokenRepository.deleteAll();
        tokenRepository.deleteAll();
        userRepository.deleteAll();
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID().toString())
                .createEntityDate(LocalDateTime.now())
                .name("Usuário da Empresa")
                .description("Usuário da Empresa")
                .email(EMAIL)
                .userName(EMAIL)
                .password(passwordEncoder.encode(SENHA))
                .isAdministrator(false)
                .accountDeactivated(false)
                .accountLocked(false)
                .changePasswordOnNextLogin(false)
                .passwordNeverExpires(true)
                .allowPasswordChange(true)
                .allowMultipleLogins(true)
                .unlimitedAccessHours(true)
                .build();
        user.setTenantId(TENANT);
        usuario = userRepository.save(user);
        filtro.setCompanyAccessResolvers(List.of(resolver));
    }

    @AfterEach
    void restaurar() {
        filtro.setCompanyValidation("off");
        filtro.setCompanyAccessResolvers(List.of());
        filtro.setExternalAuthenticators(List.of());
        SecurityContextHolder.clearContext();
        ArchbaseTenantContext.clear();
    }

    // ═══════════════════════════ off (padrão) ═══════════════════════════

    @Test
    @DisplayName("off: empresa alheia segue para o contexto, como sempre")
    void offNaoConfere() throws Exception {
        filtro.setCompanyValidation("off");
        String token = accessToken();
        Resultado r = filtrar(req -> {
            bearer(req, token);
            req.addHeader("X-COMPANY-ID", ALHEIA);
        });
        assertThat(r.cadeiaChamada).isTrue();
        assertThat(r.company).isEqualTo(ALHEIA);
    }

    // ═══════════════════════════ enforce ═══════════════════════════

    @Nested
    @DisplayName("enforce")
    class Enforce {

        @BeforeEach
        void ligar() {
            filtro.setCompanyValidation("enforce");
        }

        @Test
        @DisplayName("JWT próprio: empresa permitida segue")
        void jwtPermitida() throws Exception {
            String token = accessToken();
            Resultado r = filtrar(req -> {
                bearer(req, token);
                req.addHeader("X-COMPANY-ID", PERMITIDA);
            });
            assertThat(r.cadeiaChamada).isTrue();
            assertThat(r.company).isEqualTo(PERMITIDA);
        }

        @Test
        @DisplayName("JWT próprio: empresa alheia dá 403 COMPANY_NOT_ALLOWED")
        void jwtAlheia() throws Exception {
            String token = accessToken();
            Resultado r = filtrar(req -> {
                bearer(req, token);
                req.addHeader("X-COMPANY-ID", ALHEIA);
            });
            assertRecusa(r);
        }

        @Test
        @DisplayName("JWT próprio: empresa pela query string também é conferida")
        void jwtAlheiaNaQuery() throws Exception {
            String token = accessToken();
            Resultado r = filtrar(req -> {
                bearer(req, token);
                req.setParameter("X-COMPANY-ID", ALHEIA);
            });
            assertRecusa(r);
        }

        @Test
        @DisplayName("token de API: permitida segue, alheia dá 403")
        void viaTokenDeApi() throws Exception {
            String token = tokenDeApi();
            Resultado ok = filtrar(req -> {
                bearer(req, token);
                req.addHeader("X-COMPANY-ID", PERMITIDA);
            });
            assertThat(ok.cadeiaChamada).isTrue();
            assertThat(ok.company).isEqualTo(PERMITIDA);

            Resultado nao = filtrar(req -> {
                bearer(req, token);
                req.addHeader("X-COMPANY-ID", ALHEIA);
            });
            assertRecusa(nao);
        }

        @Test
        @DisplayName("token externo com usuário local: permitida segue, alheia dá 403")
        void tokenExterno() throws Exception {
            filtro.setExternalAuthenticators(List.of(new Externo(
                    new UsernamePasswordAuthenticationToken(usuario, null, List.of()))));
            Resultado ok = filtrar(req -> {
                bearer(req, Externo.TOKEN);
                req.addHeader("X-COMPANY-ID", PERMITIDA);
            });
            assertThat(ok.cadeiaChamada).isTrue();
            assertThat(ok.company).isEqualTo(PERMITIDA);

            Resultado nao = filtrar(req -> {
                bearer(req, Externo.TOKEN);
                req.addHeader("X-COMPANY-ID", ALHEIA);
            });
            assertRecusa(nao);
        }

        @Test
        @DisplayName("token externo sem usuário local: recusa (não há quem conferir)")
        void tokenExternoSemUsuarioLocal() throws Exception {
            TestingAuthenticationToken semUsuario = new TestingAuthenticationToken("servico", null);
            semUsuario.setAuthenticated(true);
            filtro.setExternalAuthenticators(List.of(new Externo(semUsuario)));
            Resultado r = filtrar(req -> {
                bearer(req, Externo.TOKEN);
                req.addHeader("X-COMPANY-ID", PERMITIDA);
            });
            assertRecusa(r);
        }

        @Test
        @DisplayName("sem X-COMPANY-ID: nada é conferido")
        void semEmpresa() throws Exception {
            String token = accessToken();
            Resultado r = filtrar(req -> bearer(req, token));
            assertThat(r.cadeiaChamada).isTrue();
            assertThat(r.company).isNull();
        }

        @Test
        @DisplayName("rota pública: a empresa do anônimo é descartada e marcada para o interceptor")
        void anonimoDescartado() throws Exception {
            Resultado r = filtrar(req -> req.addHeader("X-COMPANY-ID", ALHEIA));
            assertThat(r.cadeiaChamada).isTrue();
            assertThat(r.company).isNull();
            assertThat(r.request.getAttribute(ArchbaseTenantContext.COMPANY_DISCARDED_ATTRIBUTE)).isEqualTo(true);
        }

        @Test
        @DisplayName("sem resolver registrado: recusa")
        void semResolver() throws Exception {
            filtro.setCompanyAccessResolvers(List.of());
            String token = accessToken();
            Resultado r = filtrar(req -> {
                bearer(req, token);
                req.addHeader("X-COMPANY-ID", PERMITIDA);
            });
            assertRecusa(r);
        }

        @Test
        @DisplayName("resolver que lança: recusa")
        void resolverLanca() throws Exception {
            filtro.setCompanyAccessResolvers(List.of((u, c) -> {
                throw new IllegalStateException("falha no teste");
            }));
            String token = accessToken();
            Resultado r = filtrar(req -> {
                bearer(req, token);
                req.addHeader("X-COMPANY-ID", PERMITIDA);
            });
            assertRecusa(r);
        }

        @Test
        @DisplayName("com dois resolvers, basta um permitir")
        void doisResolvers() throws Exception {
            filtro.setCompanyAccessResolvers(List.of((u, c) -> false, resolver));
            String token = accessToken();
            Resultado r = filtrar(req -> {
                bearer(req, token);
                req.addHeader("X-COMPANY-ID", PERMITIDA);
            });
            assertThat(r.cadeiaChamada).isTrue();
        }
    }

    // ═══════════════════════════ warn ═══════════════════════════

    @Nested
    @DisplayName("warn")
    class Warn {

        @BeforeEach
        void ligar() {
            filtro.setCompanyValidation("warn");
        }

        @Test
        @DisplayName("empresa alheia só é registrada: segue, com a empresa no contexto")
        void alheiaSegue() throws Exception {
            String token = accessToken();
            Resultado r = filtrar(req -> {
                bearer(req, token);
                req.addHeader("X-COMPANY-ID", ALHEIA);
            });
            assertThat(r.cadeiaChamada).isTrue();
            assertThat(r.company).isEqualTo(ALHEIA);
        }

        @Test
        @DisplayName("anônimo: a empresa não é descartada")
        void anonimoMantido() throws Exception {
            Resultado r = filtrar(req -> req.addHeader("X-COMPANY-ID", ALHEIA));
            assertThat(r.cadeiaChamada).isTrue();
            assertThat(r.company).isEqualTo(ALHEIA);
            assertThat(r.request.getAttribute(ArchbaseTenantContext.COMPANY_DISCARDED_ATTRIBUTE)).isNull();
        }
    }

    // ═══════════════════════════ apoio ═══════════════════════════

    private Resultado filtrar(Consumer<MockHttpServletRequest> montar) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/qualquer-coisa");
        montar.accept(req);
        MockHttpServletResponse resp = new MockHttpServletResponse();
        Resultado r = new Resultado(req, resp);
        SecurityContextHolder.clearContext();
        ArchbaseTenantContext.clear();
        filtro.doFilter(req, resp, (q, s) -> {
            r.cadeiaChamada = true;
            r.company = ArchbaseTenantContext.getCompanyId();
        });
        return r;
    }

    private void assertRecusa(Resultado r) throws Exception {
        assertThat(r.cadeiaChamada).as("a cadeia não pode seguir numa recusa").isFalse();
        assertThat(r.resposta.getStatus()).isEqualTo(403);
        assertThat(r.resposta.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        JsonNode corpo = json.readTree(r.resposta.getContentAsString());
        assertThat(corpo.get("code").asText()).isEqualTo(ArchbaseJwtAuthenticationFilter.COMPANY_NOT_ALLOWED);
    }

    private static void bearer(MockHttpServletRequest req, String token) {
        req.addHeader("Authorization", "Bearer " + token);
    }

    private String accessToken() throws Exception {
        var resposta = mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", EMAIL, "password", SENHA))))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(resposta.getResponse().getContentAsString()).get("access_token").asText();
    }

    private String tokenDeApi() {
        String token = UUID.randomUUID().toString();
        apiTokenRepository.save(ApiTokenEntity.builder()
                .id(UUID.randomUUID().toString())
                .createEntityDate(LocalDateTime.now())
                .createdByUser(EMAIL)
                .tenantId(TENANT)
                .name("token-empresa")
                .description("token-empresa")
                .user(usuario)
                .tokenHash(ApiTokenHasher.hash(token))
                .revoked(false)
                .activated(true)
                .expirationDate(LocalDateTime.now().plusDays(1))
                .build());
        return token;
    }

    static final class Resultado {
        final MockHttpServletRequest request;
        final MockHttpServletResponse resposta;
        boolean cadeiaChamada;
        String company;

        Resultado(MockHttpServletRequest request, MockHttpServletResponse resposta) {
            this.request = request;
            this.resposta = resposta;
        }
    }

    /** Autenticador externo que aceita um token fixo no tenant do teste. Sem {@code @Component}. */
    static final class Externo implements ExternalBearerAuthenticator {
        static final String TOKEN = "eyJhbGciOiJub25lIn0.eyJpc3MiOiJleHRlcm5vIn0.";
        private final Authentication autenticacao;

        Externo(Authentication autenticacao) {
            this.autenticacao = autenticacao;
        }

        @Override
        public boolean supports(String token) {
            return TOKEN.equals(token);
        }

        @Override
        public ExternalTokenCheck check(String token, HttpServletRequest request) {
            return ExternalTokenCheck.accepted(TENANT, Map.of("sub", "x"));
        }

        @Override
        public Authentication authenticate(ExternalTokenCheck check, HttpServletRequest request) {
            return autenticacao;
        }
    }
}
