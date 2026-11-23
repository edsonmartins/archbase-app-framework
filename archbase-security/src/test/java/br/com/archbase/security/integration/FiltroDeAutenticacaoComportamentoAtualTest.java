package br.com.archbase.security.integration;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.security.auth.external.ExternalAuthenticationPolicy;
import br.com.archbase.security.auth.external.ExternalAuthenticationRejectedException;
import br.com.archbase.security.auth.external.ExternalBearerAuthenticator;
import br.com.archbase.security.auth.external.ExternalTokenCheck;
import br.com.archbase.security.config.ArchbaseJwtAuthenticationFilter;
import br.com.archbase.security.persistence.ApiTokenEntity;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.AccessTokenJpaRepository;
import br.com.archbase.security.repository.ApiTokenRepository;
import br.com.archbase.security.repository.UserJpaRepository;
import br.com.archbase.security.service.ArchbaseJwtService;
import br.com.archbase.security.util.ApiTokenHasher;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fase 0 do suporte a IdP externo (PROPOSTA_IDP_EXTERNO_OIDC.md): fotografa o comportamento ATUAL do
 * {@link ArchbaseJwtAuthenticationFilter}, ramo a ramo, antes de qualquer mudança no core.
 *
 * <p><b>Estes testes não julgam o comportamento — registram.</b> Alguns cenários documentam
 * escolhas discutíveis (ex.: token de API com {@code X-TENANT-ID} divergente dá 401, não 403). Se um
 * deles quebrar, a mudança alterou o que aplicações em produção recebem hoje: ou é regressão, ou é
 * mudança de comportamento que precisa ser deliberada, documentada e ter flag.
 *
 * <p><b>Como.</b> Chama o filtro real (o bean do contexto, com banco, serviço de JWT e repositórios
 * reais) com uma requisição montada à mão e uma {@code FilterChain} que só registra o que o filtro
 * deixou pronto para a aplicação: a {@link Authentication}, o tenant e a company do contexto. "Não
 * autenticou" aparece como cadeia chamada sem autenticação — o 401 em si é do entry point, que já
 * tem teste em {@link AutenticacaoIntegrationTest}.
 *
 * <p>O bean é compartilhado pelo cache de contexto; tudo que é alterado nele (flags e autenticadores
 * externos) é restaurado no {@code @AfterEach}.
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
@DisplayName("Filtro de autenticação: comportamento atual (fase 0)")
class FiltroDeAutenticacaoComportamentoAtualTest {

    private static final String TENANT = ArchbaseSecurityTestApplication.TENANT;
    private static final String OUTRO_TENANT = "outro-tenant";
    private static final String EMAIL = "filtro@fase0.com.br";
    private static final String SENHA = "senha-de-teste";

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
    @Autowired
    ArchbaseJwtService jwtService;
    @Autowired
    br.com.archbase.security.service.ArchbaseUserService userService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private UserEntity usuario;

    @BeforeEach
    void preparar() {
        apiTokenRepository.deleteAll();
        tokenRepository.deleteAll();
        userRepository.deleteAll();

        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID().toString())
                .createEntityDate(LocalDateTime.now())
                .name("Usuário do Filtro")
                .description("Usuário do Filtro")
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
    }

    @AfterEach
    void restaurar() {
        filtro.setExternalAuthenticators(List.of());
        filtro.setExternalPolicy(null);
        ReflectionTestUtils.setField(filtro, "acceptTokenQueryParam", true);
        ReflectionTestUtils.setField(filtro, "acceptTenantQueryParam", true);
        ReflectionTestUtils.setField(filtro, "defaultTenantId", TENANT);
        SecurityContextHolder.clearContext();
        ArchbaseTenantContext.clear();
    }

    // ═══════════════════════════ JWT próprio do Archbase ═══════════════════════════

    @Nested
    @DisplayName("JWT próprio (HS256)")
    class JwtProprio {

        @Test
        @DisplayName("Bearer válido autentica com UsernamePasswordAuthenticationToken cujo principal é UserEntity")
        void bearerValido() throws Exception {
            String token = accessToken();
            Resultado r = filtrar(req -> bearer(req, token));

            assertThat(r.cadeiaChamada).isTrue();
            // A proposta troca os casts por CurrentUser: este é o tipo que ela precisa continuar aceitando.
            assertThat(r.autenticacao).isInstanceOf(UsernamePasswordAuthenticationToken.class);
            assertThat(r.autenticacao.getPrincipal()).isInstanceOf(UserEntity.class);
            assertThat(((UserEntity) r.autenticacao.getPrincipal()).getEmail()).isEqualTo(EMAIL);
            assertThat(r.tenant).isEqualTo(TENANT);
        }

        @Test
        @DisplayName("sem X-TENANT-ID, o tenant do contexto é o do token")
        void tenantDoToken() throws Exception {
            String token = accessToken();
            Resultado r = filtrar(req -> bearer(req, token));
            assertThat(r.tenant).isEqualTo(TENANT);
        }

        @Test
        @DisplayName("X-TENANT-ID igual ao do token segue normalmente")
        void tenantIgual() throws Exception {
            String token = accessToken();
            Resultado r = filtrar(req -> {
                bearer(req, token);
                req.addHeader(ArchbaseJwtAuthenticationFilter.X_TENANT_ID, TENANT);
            });
            assertThat(r.autenticado()).isTrue();
            assertThat(r.tenant).isEqualTo(TENANT);
        }

        @Test
        @DisplayName("X-TENANT-ID divergente do token: NÃO autentica (401), e não 403 como o código anuncia")
        void tenantDivergenteNaoAutentica() throws Exception {
            // Comportamento atual, registrado como está: o header entra no contexto ANTES de o token
            // ser conferido, e o token (AccessTokenEntity, @TenantId) é procurado no tenant do header —
            // onde não existe. Sem autenticação, o bloco de reconciliação que responderia 403 nem roda.
            // O acesso é negado do mesmo jeito; muda só o status que o cliente vê.
            String token = accessToken();
            Resultado r = filtrar(req -> {
                bearer(req, token);
                req.addHeader(ArchbaseJwtAuthenticationFilter.X_TENANT_ID, OUTRO_TENANT);
            });
            assertThat(r.cadeiaChamada).isTrue();
            assertThat(r.autenticado()).isFalse();
            assertThat(r.resposta.getStatus()).isEqualTo(200); // nada escrito pelo filtro; o 401 é do entry point
        }

        @Test
        @DisplayName("X-COMPANY-ID do header vai para o contexto")
        void companyDoHeader() throws Exception {
            String token = accessToken();
            Resultado r = filtrar(req -> {
                bearer(req, token);
                req.addHeader(ArchbaseJwtAuthenticationFilter.X_COMPANY_ID, "empresa-1");
            });
            assertThat(r.company).isEqualTo("empresa-1");
        }

        @Test
        @DisplayName("refresh token apresentado como Bearer não autentica")
        void refreshNaoAutentica() throws Exception {
            String refresh = login().get("refresh_token").asText();
            Resultado r = filtrar(req -> bearer(req, refresh));
            assertThat(r.cadeiaChamada).isTrue();
            assertThat(r.autenticado()).isFalse();
        }

        @Test
        @DisplayName("desafio de MFA apresentado como Bearer não autentica")
        void desafioMfaNaoAutentica() throws Exception {
            String desafio = jwtService.generateMfaChallengeToken(usuario).token();
            Resultado r = filtrar(req -> bearer(req, desafio));
            assertThat(r.autenticado()).isFalse();
        }

        @Test
        @DisplayName("JWT assinado com outra chave não autentica e não derruba a requisição")
        void assinaturaErrada() throws Exception {
            String forjado = Jwts.builder()
                    .subject(EMAIL)
                    .claim("token_use", "access")
                    .claim("tenantId", TENANT)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 60_000))
                    .signWith(Keys.hmacShaKeyFor("outra-chave-com-pelo-menos-32-bytes!!".getBytes(StandardCharsets.UTF_8)))
                    .compact();
            Resultado r = filtrar(req -> bearer(req, forjado));
            assertThat(r.autenticado()).isFalse();
        }

        @Test
        @DisplayName("JWT válido que não está na tabela de tokens não autentica")
        void naoPersistidoNaoAutentica() throws Exception {
            String token = jwtService.generateToken(usuario).token();
            Resultado r = filtrar(req -> bearer(req, token));
            assertThat(r.autenticado()).isFalse();
        }

        @Test
        @DisplayName("Authorization em formato desconhecido é ignorado, sem erro")
        void formatoDesconhecido() throws Exception {
            Resultado r = filtrar(req -> req.addHeader("Authorization", "Basic dXN1YXJpbzpzZW5oYQ=="));
            assertThat(r.cadeiaChamada).isTrue();
            assertThat(r.autenticado()).isFalse();
        }

        @Test
        @DisplayName("sem credencial: cadeia segue sem autenticação e com o tenant padrão")
        void semCredencial() throws Exception {
            Resultado r = filtrar(req -> { });
            assertThat(r.cadeiaChamada).isTrue();
            assertThat(r.autenticado()).isFalse();
            assertThat(r.tenant).isEqualTo(TENANT);
        }

        @Test
        @DisplayName("sem credencial e sem tenant padrão: contexto sem tenant")
        void semTenantPadrao() throws Exception {
            ReflectionTestUtils.setField(filtro, "defaultTenantId", "");
            Resultado r = filtrar(req -> { });
            assertThat(r.tenant).isNull();
        }
    }

    // ═══════════════════════════ credencial e tenant pela query string ═══════════════════════════

    @Nested
    @DisplayName("Query string (?token= e ?X-TENANT-ID=)")
    class QueryString {

        @Test
        @DisplayName("?token= autentica quando não há header (padrão: aceito)")
        void tokenNaQuery() throws Exception {
            String token = accessToken();
            Resultado r = filtrar(req -> req.setParameter("token", token));
            assertThat(r.autenticado()).isTrue();
        }

        @Test
        @DisplayName("com accept-token-query-param=false, ?token= é ignorado")
        void tokenNaQueryDesligado() throws Exception {
            String token = accessToken();
            ReflectionTestUtils.setField(filtro, "acceptTokenQueryParam", false);
            Resultado r = filtrar(req -> req.setParameter("token", token));
            assertThat(r.autenticado()).isFalse();
        }

        @Test
        @DisplayName("header Authorization tem precedência sobre ?token=")
        void headerVenceQuery() throws Exception {
            String token = accessToken();
            Resultado r = filtrar(req -> {
                req.addHeader("Authorization", "Basic xxx");
                req.setParameter("token", token);
            });
            assertThat(r.autenticado()).isFalse();
        }

        @Test
        @DisplayName("?X-TENANT-ID= é lido (padrão: aceito) e, divergente do token, também não autentica")
        void tenantNaQueryDivergente() throws Exception {
            // Mesmo mecanismo do header divergente (ver JwtProprio#tenantDivergenteNaoAutentica). Prova
            // que a query string é de fato lida: com a flag desligada (teste abaixo) o mesmo pedido autentica.
            String token = accessToken();
            Resultado r = filtrar(req -> {
                bearer(req, token);
                req.setParameter(ArchbaseJwtAuthenticationFilter.X_TENANT_ID, OUTRO_TENANT);
            });
            assertThat(r.cadeiaChamada).isTrue();
            assertThat(r.autenticado()).isFalse();
        }

        @Test
        @DisplayName("com accept-query-param=false, ?X-TENANT-ID= e ?X-COMPANY-ID= são ignorados")
        void tenantNaQueryDesligado() throws Exception {
            String token = accessToken();
            ReflectionTestUtils.setField(filtro, "acceptTenantQueryParam", false);
            Resultado r = filtrar(req -> {
                bearer(req, token);
                req.setParameter(ArchbaseJwtAuthenticationFilter.X_TENANT_ID, OUTRO_TENANT);
                req.setParameter(ArchbaseJwtAuthenticationFilter.X_COMPANY_ID, "empresa-1");
            });
            assertThat(r.cadeiaChamada).isTrue();
            assertThat(r.autenticado()).isTrue();
            assertThat(r.tenant).isEqualTo(TENANT);
            assertThat(r.company).isNull();
        }

        @Test
        @DisplayName("?X-COMPANY-ID= vai para o contexto quando não há header (padrão: aceito)")
        void companyNaQuery() throws Exception {
            String token = accessToken();
            Resultado r = filtrar(req -> {
                bearer(req, token);
                req.setParameter(ArchbaseJwtAuthenticationFilter.X_COMPANY_ID, "empresa-2");
            });
            assertThat(r.company).isEqualTo("empresa-2");
        }
    }

    // ═══════════════════════════ token de API ═══════════════════════════

    @Nested
    @DisplayName("Token de API (UUID)")
    class TokenDeApi {

        @Test
        @DisplayName("como Bearer autentica e alinha o tenant ao do token")
        void comoBearer() throws Exception {
            String token = tokenDeApi(true);
            Resultado r = filtrar(req -> bearer(req, token));
            assertThat(r.autenticado()).isTrue();
            assertThat(r.autenticacao).isInstanceOf(UsernamePasswordAuthenticationToken.class);
            assertThat(r.autenticacao.getPrincipal()).isInstanceOf(UserEntity.class);
            assertThat(r.tenant).isEqualTo(TENANT);
        }

        @Test
        @DisplayName("cru no header Authorization (sem 'Bearer') autentica")
        void cruNoHeader() throws Exception {
            String token = tokenDeApi(true);
            Resultado r = filtrar(req -> req.addHeader("Authorization", token));
            assertThat(r.autenticado()).isTrue();
        }

        @Test
        @DisplayName("em ?token= autentica")
        void naQuery() throws Exception {
            String token = tokenDeApi(true);
            Resultado r = filtrar(req -> req.setParameter("token", token));
            assertThat(r.autenticado()).isTrue();
        }

        @Test
        @DisplayName("não ativado não autentica")
        void naoAtivado() throws Exception {
            String token = tokenDeApi(false);
            Resultado r = filtrar(req -> bearer(req, token));
            assertThat(r.autenticado()).isFalse();
        }

        @Test
        @DisplayName("UUID desconhecido não autentica")
        void desconhecido() throws Exception {
            Resultado r = filtrar(req -> bearer(req, UUID.randomUUID().toString()));
            assertThat(r.autenticado()).isFalse();
        }

        @Test
        @DisplayName("X-TENANT-ID divergente: NÃO autentica (401), em vez de 403 como no JWT")
        void tenantDivergenteDa401() throws Exception {
            // Comportamento atual: o validateToken filtra pelo tenant do contexto (que veio do header),
            // então o token simplesmente não é encontrado. Registrado como está.
            String token = tokenDeApi(true);
            Resultado r = filtrar(req -> {
                bearer(req, token);
                req.addHeader(ArchbaseJwtAuthenticationFilter.X_TENANT_ID, OUTRO_TENANT);
            });
            assertThat(r.cadeiaChamada).isTrue();
            assertThat(r.autenticado()).isFalse();
        }

        @Test
        @DisplayName("token de API nunca é oferecido ao autenticador externo")
        void naoPassaPeloExterno() throws Exception {
            String token = tokenDeApi(true);
            ExternoFalso externo = new ExternoFalso().reconhece(true);
            filtro.setExternalAuthenticators(List.of(externo));

            Resultado r = filtrar(req -> bearer(req, token));

            assertThat(externo.supportsChamado).isFalse();
            assertThat(r.autenticado()).isTrue();
        }
    }

    // ═══════════════════════════ SPI de autenticador externo ═══════════════════════════

    @Nested
    @DisplayName("Autenticador externo (ExternalBearerAuthenticator)")
    class AutenticadorExterno {

        private static final String TOKEN_EXTERNO = "eyJhbGciOiJFZERTQSJ9.eyJzdWIiOiJ4In0.assinatura";

        @Test
        @DisplayName("supports=false: o fluxo próprio segue como se o externo não existisse")
        void naoReconhece() throws Exception {
            String token = accessToken();
            ExternoFalso externo = new ExternoFalso().reconhece(false);
            filtro.setExternalAuthenticators(List.of(externo));

            Resultado r = filtrar(req -> bearer(req, token));

            assertThat(externo.supportsChamado).isTrue();
            assertThat(externo.checkChamado).isFalse();
            assertThat(r.autenticado()).isTrue();
        }

        @Test
        @DisplayName("supports lança exceção: ignorado, o fluxo próprio segue")
        void supportsLanca() throws Exception {
            String token = accessToken();
            ExternoFalso externo = new ExternoFalso().supportsLanca();
            filtro.setExternalAuthenticators(List.of(externo));

            Resultado r = filtrar(req -> bearer(req, token));

            assertThat(r.autenticado()).isTrue();
        }

        @Test
        @DisplayName("o primeiro que reconhece decide; os seguintes não são consultados")
        void primeiroDecide() throws Exception {
            ExternoFalso primeiro = new ExternoFalso().reconhece(true).aceita(TENANT);
            ExternoFalso segundo = new ExternoFalso().reconhece(true).aceita(TENANT);
            filtro.setExternalAuthenticators(List.of(primeiro, segundo));

            filtrar(req -> bearer(req, TOKEN_EXTERNO));

            assertThat(primeiro.checkChamado).isTrue();
            assertThat(segundo.supportsChamado).isFalse();
        }

        @Test
        @DisplayName("aceito: autentica com a Authentication do externo, tenant do check e company do header")
        void aceito() throws Exception {
            ExternoFalso externo = new ExternoFalso().reconhece(true).aceita(TENANT);
            filtro.setExternalAuthenticators(List.of(externo));

            Resultado r = filtrar(req -> {
                bearer(req, TOKEN_EXTERNO);
                req.addHeader(ArchbaseJwtAuthenticationFilter.X_COMPANY_ID, "empresa-3");
            });

            assertThat(r.cadeiaChamada).isTrue();
            assertThat(r.autenticacao).isSameAs(externo.autenticacao);
            assertThat(r.tenant).isEqualTo(TENANT);
            assertThat(r.company).isEqualTo("empresa-3");
            assertThat(externo.tenantNoAuthenticate).isEqualTo(TENANT);
        }

        @Test
        @DisplayName("aceito sem tenant no check: usa o X-TENANT-ID")
        void tenantDoHeader() throws Exception {
            ExternoFalso externo = new ExternoFalso().reconhece(true).aceita(null);
            filtro.setExternalAuthenticators(List.of(externo));

            Resultado r = filtrar(req -> {
                bearer(req, TOKEN_EXTERNO);
                req.addHeader(ArchbaseJwtAuthenticationFilter.X_TENANT_ID, OUTRO_TENANT);
            });

            assertThat(r.autenticado()).isTrue();
            assertThat(r.tenant).isEqualTo(OUTRO_TENANT);
        }

        @Test
        @DisplayName("aceito sem tenant no check e sem header: 403 TENANT_REQUIRED (tenant padrão nunca é usado)")
        void tenantObrigatorio() throws Exception {
            ExternoFalso externo = new ExternoFalso().reconhece(true).aceita(null);
            filtro.setExternalAuthenticators(List.of(externo));

            Resultado r = filtrar(req -> bearer(req, TOKEN_EXTERNO));

            assertRecusa(r, 403, "TENANT_REQUIRED");
            assertThat(externo.recusas).containsExactly("TENANT_REQUIRED");
        }

        @Test
        @DisplayName("X-TENANT-ID divergente do check: 403 TENANT_MISMATCH")
        void tenantDivergente() throws Exception {
            ExternoFalso externo = new ExternoFalso().reconhece(true).aceita(TENANT);
            filtro.setExternalAuthenticators(List.of(externo));

            Resultado r = filtrar(req -> {
                bearer(req, TOKEN_EXTERNO);
                req.addHeader(ArchbaseJwtAuthenticationFilter.X_TENANT_ID, OUTRO_TENANT);
            });

            assertRecusa(r, 403, "TENANT_MISMATCH");
            assertThat(externo.authenticateChamado).isFalse();
        }

        @Test
        @DisplayName("check recusado: responde o status e o código do check, com WWW-Authenticate no 401")
        void checkRecusado() throws Exception {
            ExternoFalso externo = new ExternoFalso().reconhece(true).recusa(401, "TOKEN_EXPIRED");
            filtro.setExternalAuthenticators(List.of(externo));

            Resultado r = filtrar(req -> bearer(req, TOKEN_EXTERNO));

            assertRecusa(r, 401, "TOKEN_EXPIRED");
            assertThat(r.resposta.getHeader("WWW-Authenticate")).isEqualTo("Bearer error=\"invalid_token\"");
            assertThat(externo.recusas).containsExactly("TOKEN_EXPIRED");
        }

        @Test
        @DisplayName("token reconhecido e recusado NÃO cai no fluxo próprio")
        void recusadoNaoCaiNoLegado() throws Exception {
            String tokenProprio = accessToken();
            ExternoFalso externo = new ExternoFalso().reconhece(true).recusa(401, "INVALID_TOKEN");
            filtro.setExternalAuthenticators(List.of(externo));

            Resultado r = filtrar(req -> bearer(req, tokenProprio));

            assertRecusa(r, 401, "INVALID_TOKEN");
        }

        @Test
        @DisplayName("check lança exceção: 401 INVALID_TOKEN")
        void checkLanca() throws Exception {
            ExternoFalso externo = new ExternoFalso().reconhece(true).checkLanca();
            filtro.setExternalAuthenticators(List.of(externo));

            assertRecusa(filtrar(req -> bearer(req, TOKEN_EXTERNO)), 401, "INVALID_TOKEN");
        }

        @Test
        @DisplayName("check devolve null: 401 INVALID_TOKEN")
        void checkNulo() throws Exception {
            ExternoFalso externo = new ExternoFalso().reconhece(true);
            filtro.setExternalAuthenticators(List.of(externo));

            assertRecusa(filtrar(req -> bearer(req, TOKEN_EXTERNO)), 401, "INVALID_TOKEN");
        }

        @Test
        @DisplayName("authenticate devolve null: 401 INVALID_TOKEN")
        void authenticateNulo() throws Exception {
            ExternoFalso externo = new ExternoFalso().reconhece(true).aceita(TENANT).semAutenticacao();
            filtro.setExternalAuthenticators(List.of(externo));

            assertRecusa(filtrar(req -> bearer(req, TOKEN_EXTERNO)), 401, "INVALID_TOKEN");
        }

        @Test
        @DisplayName("authenticate lança ExternalAuthenticationRejectedException: status e código dela")
        void authenticateRecusa() throws Exception {
            ExternoFalso externo = new ExternoFalso().reconhece(true).aceita(TENANT)
                    .authenticateRecusa(403, "NO_SEAT");
            filtro.setExternalAuthenticators(List.of(externo));

            Resultado r = filtrar(req -> bearer(req, TOKEN_EXTERNO));

            assertRecusa(r, 403, "NO_SEAT");
            assertThat(r.resposta.getHeader("WWW-Authenticate")).isNull();
        }

        @Test
        @DisplayName("authenticate lança outra exceção: 401 INVALID_TOKEN")
        void authenticateLanca() throws Exception {
            ExternoFalso externo = new ExternoFalso().reconhece(true).aceita(TENANT).authenticateLanca();
            filtro.setExternalAuthenticators(List.of(externo));

            assertRecusa(filtrar(req -> bearer(req, TOKEN_EXTERNO)), 401, "INVALID_TOKEN");
        }

        @Test
        @DisplayName("onRejected que lança não muda a resposta")
        void onRejectedLanca() throws Exception {
            ExternoFalso externo = new ExternoFalso().reconhece(true).recusa(401, "INVALID_TOKEN").onRejectedLanca();
            filtro.setExternalAuthenticators(List.of(externo));

            assertRecusa(filtrar(req -> bearer(req, TOKEN_EXTERNO)), 401, "INVALID_TOKEN");
        }

        @Test
        @DisplayName("o externo também recebe o token vindo de ?token=")
        void tokenNaQuery() throws Exception {
            ExternoFalso externo = new ExternoFalso().reconhece(true).aceita(TENANT);
            filtro.setExternalAuthenticators(List.of(externo));

            Resultado r = filtrar(req -> req.setParameter("token", TOKEN_EXTERNO));

            assertThat(externo.checkChamado).isTrue();
            assertThat(r.autenticado()).isTrue();
        }
    }

    // ═══════════════════════════ ExternalAuthenticationPolicy ═══════════════════════════

    @Nested
    @DisplayName("Política do provedor externo (ExternalAuthenticationPolicy)")
    class Politica {

        @Test
        @DisplayName("acceptLegacyJwt=false: JWT próprio não autentica")
        void legadoRecusado() throws Exception {
            String token = accessToken();
            filtro.setExternalPolicy(politica(false, true));

            Resultado r = filtrar(req -> bearer(req, token));

            assertThat(r.cadeiaChamada).isTrue();
            assertThat(r.autenticado()).isFalse();
        }

        @Test
        @DisplayName("acceptLegacyJwt=false não afeta token de API")
        void legadoRecusadoApiTokenSegue() throws Exception {
            String token = tokenDeApi(true);
            filtro.setExternalPolicy(politica(false, true));

            Resultado r = filtrar(req -> bearer(req, token));

            assertThat(r.autenticado()).isTrue();
        }

        @Test
        @DisplayName("allowDefaultTenant=false, sem header: tenant vem do claim do JWT")
        void semTenantPadraoUsaClaim() throws Exception {
            String token = accessToken();
            filtro.setExternalPolicy(politica(true, false));

            Resultado r = filtrar(req -> bearer(req, token));

            assertThat(r.autenticado()).isTrue();
            assertThat(r.tenant).isEqualTo(TENANT);
        }

        @Test
        @DisplayName("allowDefaultTenant=false, sem credencial e sem header: contexto sem tenant")
        void semTenantPadraoSemCredencial() throws Exception {
            filtro.setExternalPolicy(politica(true, false));

            Resultado r = filtrar(req -> { });

            assertThat(r.tenant).isNull();
        }
    }

    // ═══════════════════════════ troca de senha (cast que a fase 1 troca) ═══════════════════════════

    @Nested
    @DisplayName("Troca da própria senha")
    class TrocaDeSenha {

        // ArchbaseUserService.changePassword(request, Principal) faz
        // (UserEntity) ((UsernamePasswordAuthenticationToken) principal).getPrincipal(). A fase 1 troca esse
        // cast por CurrentUser; estes cenários passam ao serviço a Authentication REAL que o filtro monta
        // para cada credencial de hoje, e garantem que o resultado continua o mesmo.

        @Test
        @DisplayName("com a Authentication do JWT próprio: troca a senha")
        void comJwtProprio() throws Exception {
            String token = accessToken();
            Authentication autenticacao = filtrar(req -> bearer(req, token)).autenticacao;

            userService.changePassword(pedido(SENHA, "nova-senha-fase0"), autenticacao);

            assertThat(senhaAtualE("nova-senha-fase0")).isTrue();
        }

        @Test
        @DisplayName("com a Authentication do token de API: troca a senha")
        void comTokenDeApi() throws Exception {
            String token = tokenDeApi(true);
            Authentication autenticacao = filtrar(req -> bearer(req, token)).autenticacao;

            userService.changePassword(pedido(SENHA, "nova-senha-fase0"), autenticacao);

            assertThat(senhaAtualE("nova-senha-fase0")).isTrue();
        }

        @Test
        @DisplayName("senha atual errada: IllegalStateException e a senha não muda")
        void senhaAtualErrada() throws Exception {
            String token = accessToken();
            Authentication autenticacao = filtrar(req -> bearer(req, token)).autenticacao;

            org.assertj.core.api.Assertions.assertThatThrownBy(
                            () -> userService.changePassword(pedido("errada", "nova-senha-fase0"), autenticacao))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(senhaAtualE(SENHA)).isTrue();
        }

        @Test
        @DisplayName("PATCH /api/v1/user troca a senha de ponta a ponta")
        void endpointTrocaASenha() throws Exception {
            // Até a correção respondia 500: ChangePasswordRequest só tinha @Builder, sem construtor vazio, e o
            // Jackson não o desserializava ("Type definition error"). Este cenário é o que pegaria a volta disso.
            String token = accessToken();
            int status = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .patch("/api/v1/user")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "currentPassword", SENHA,
                                    "newPassword", "nova-senha-fase0",
                                    "confirmationPassword", "nova-senha-fase0"))))
                    .andReturn().getResponse().getStatus();

            assertThat(status).isEqualTo(200);
            assertThat(senhaAtualE("nova-senha-fase0")).isTrue();
        }

        @Test
        @DisplayName("PATCH /api/v1/user com a senha atual errada não troca")
        void endpointSenhaAtualErrada() throws Exception {
            String token = accessToken();
            int status = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .patch("/api/v1/user")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "currentPassword", "errada",
                                    "newPassword", "nova-senha-fase0",
                                    "confirmationPassword", "nova-senha-fase0"))))
                    .andReturn().getResponse().getStatus();

            assertThat(status).isNotEqualTo(200);
            assertThat(senhaAtualE(SENHA)).isTrue();
        }

        private br.com.archbase.security.auth.ChangePasswordRequest pedido(String atual, String nova) {
            return br.com.archbase.security.auth.ChangePasswordRequest.builder()
                    .currentPassword(atual)
                    .newPassword(nova)
                    .confirmationPassword(nova)
                    .build();
        }

        private boolean senhaAtualE(String senha) {
            return passwordEncoder.matches(senha, userRepository.findByEmail(EMAIL).orElseThrow().getPassword());
        }
    }

    // ═══════════════════════════ apoio ═══════════════════════════

    private Resultado filtrar(java.util.function.Consumer<MockHttpServletRequest> montar) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/qualquer-coisa");
        montar.accept(req);
        MockHttpServletResponse resp = new MockHttpServletResponse();
        Resultado r = new Resultado(resp);
        SecurityContextHolder.clearContext();
        ArchbaseTenantContext.clear();
        filtro.doFilter(req, resp, (q, s) -> {
            r.cadeiaChamada = true;
            r.autenticacao = SecurityContextHolder.getContext().getAuthentication();
            r.tenant = ArchbaseTenantContext.getTenantId();
            r.company = ArchbaseTenantContext.getCompanyId();
        });
        return r;
    }

    private void assertRecusa(Resultado r, int status, String code) throws Exception {
        assertThat(r.cadeiaChamada).as("a cadeia não pode seguir numa recusa").isFalse();
        assertThat(r.resposta.getStatus()).isEqualTo(status);
        assertThat(r.resposta.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        JsonNode corpo = objectMapper.readTree(r.resposta.getContentAsString());
        assertThat(corpo.get("status").asInt()).isEqualTo(status);
        assertThat(corpo.get("code").asText()).isEqualTo(code);
        assertThat(corpo.get("error").asText()).isEqualTo(code);
        assertThat(corpo.has("message")).isTrue();
    }

    private static void bearer(MockHttpServletRequest req, String token) {
        req.addHeader("Authorization", "Bearer " + token);
    }

    private String accessToken() throws Exception {
        return login().get("access_token").asText();
    }

    private JsonNode login() throws Exception {
        var resposta = mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", EMAIL, "password", SENHA))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resposta.getResponse().getContentAsString());
    }

    private String tokenDeApi(boolean ativado) {
        String token = UUID.randomUUID().toString();
        ApiTokenEntity entidade = ApiTokenEntity.builder()
                .id(UUID.randomUUID().toString())
                .createEntityDate(LocalDateTime.now())
                .createdByUser(EMAIL)
                .tenantId(TENANT)
                .name("token-fase0")
                .description("token-fase0")
                .user(usuario)
                .tokenHash(ApiTokenHasher.hash(token))
                .revoked(false)
                .activated(ativado)
                .expirationDate(LocalDateTime.now().plusDays(1))
                .build();
        apiTokenRepository.save(entidade);
        return token;
    }

    private static ExternalAuthenticationPolicy politica(boolean aceitaLegado, boolean permiteTenantPadrao) {
        return new ExternalAuthenticationPolicy() {
            @Override
            public boolean acceptLegacyJwt() {
                return aceitaLegado;
            }

            @Override
            public boolean allowDefaultTenant() {
                return permiteTenantPadrao;
            }
        };
    }

    static final class Resultado {
        final MockHttpServletResponse resposta;
        boolean cadeiaChamada;
        Authentication autenticacao;
        String tenant;
        String company;

        Resultado(MockHttpServletResponse resposta) {
            this.resposta = resposta;
        }

        boolean autenticado() {
            return autenticacao != null && autenticacao.isAuthenticated();
        }
    }

    /** Autenticador externo programável. Sem {@code @Component}: o scan do contexto de teste o pegaria. */
    static final class ExternoFalso implements ExternalBearerAuthenticator {
        boolean reconhece;
        boolean supportsLanca;
        Function<String, ExternalTokenCheck> check = t -> null;
        boolean checkLanca;
        boolean semAutenticacao;
        RuntimeException erroNoAuthenticate;
        boolean onRejectedLanca;

        final Authentication autenticacao = new TestingAuthenticationToken("externo", null, "ROLE_EXTERNO");
        boolean supportsChamado;
        boolean checkChamado;
        boolean authenticateChamado;
        String tenantNoAuthenticate;
        final List<String> recusas = new ArrayList<>();

        ExternoFalso() {
            autenticacao.setAuthenticated(true);
        }

        ExternoFalso reconhece(boolean valor) {
            this.reconhece = valor;
            return this;
        }

        ExternoFalso supportsLanca() {
            this.supportsLanca = true;
            return this;
        }

        ExternoFalso aceita(String tenant) {
            this.check = t -> ExternalTokenCheck.accepted(tenant, Map.of("sub", "x"));
            return this;
        }

        ExternoFalso recusa(int status, String code) {
            this.check = t -> ExternalTokenCheck.rejected(status, code, "recusado no teste");
            return this;
        }

        ExternoFalso checkLanca() {
            this.checkLanca = true;
            return this;
        }

        ExternoFalso semAutenticacao() {
            this.semAutenticacao = true;
            return this;
        }

        ExternoFalso authenticateRecusa(int status, String code) {
            this.erroNoAuthenticate = new ExternalAuthenticationRejectedException(status, code, "recusado no teste");
            return this;
        }

        ExternoFalso authenticateLanca() {
            this.erroNoAuthenticate = new IllegalStateException("falha no teste");
            return this;
        }

        ExternoFalso onRejectedLanca() {
            this.onRejectedLanca = true;
            return this;
        }

        @Override
        public boolean supports(String token) {
            supportsChamado = true;
            if (supportsLanca) {
                throw new IllegalStateException("falha no supports");
            }
            return reconhece;
        }

        @Override
        public ExternalTokenCheck check(String token, HttpServletRequest request) {
            checkChamado = true;
            if (checkLanca) {
                throw new IllegalStateException("falha no check");
            }
            return check.apply(token);
        }

        @Override
        public Authentication authenticate(ExternalTokenCheck check, HttpServletRequest request) {
            authenticateChamado = true;
            tenantNoAuthenticate = ArchbaseTenantContext.getTenantId();
            if (erroNoAuthenticate != null) {
                throw erroNoAuthenticate;
            }
            return semAutenticacao ? null : autenticacao;
        }

        @Override
        public void onRejected(String code, HttpServletRequest request) {
            recusas.add(code);
            if (onRejectedLanca) {
                throw new IllegalStateException("falha no onRejected");
            }
        }
    }
}
