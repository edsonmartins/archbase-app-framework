package br.com.archbase.security.integration;

import br.com.archbase.security.adapter.SecurityAdapter;
import br.com.archbase.security.auth.ArchbaseCurrentUser;
import br.com.archbase.security.auth.ChangePasswordRequest;
import br.com.archbase.security.auth.PasswordResetRequest;
import br.com.archbase.security.auth.external.ExternalAuthenticationPolicy;
import br.com.archbase.security.config.ArchbaseJwtAuthenticationFilter;
import br.com.archbase.security.persistence.PasswordResetTokenEntity;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.AccessTokenJpaRepository;
import br.com.archbase.security.repository.ApiTokenRepository;
import br.com.archbase.security.repository.PasswordResetTokenJpaRepository;
import br.com.archbase.security.repository.UserJpaRepository;
import br.com.archbase.security.service.ArchbaseAuthenticationService;
import br.com.archbase.security.service.ArchbaseUserService;
import br.com.archbase.security.spi.ArchbaseLocalCredentialPolicy;
import br.com.archbase.validation.exception.ArchbaseValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Fase 1a do IdP externo (PROPOSTA_IDP_EXTERNO_OIDC.md): as mudanças compatíveis no core.
 *
 * <ul>
 * <li>política de provedor externo agregada (lista, combinação conservadora);</li>
 * <li>{@link ArchbaseCurrentUser} no lugar dos casts que quebravam com autenticação externa;</li>
 * <li>{@link ArchbaseLocalCredentialPolicy}: senha local desligável por conta.</li>
 * </ul>
 *
 * <p>Sem nenhum bean novo registrado, nada disso muda o comportamento — quem prova é
 * {@link FiltroDeAutenticacaoComportamentoAtualTest}, que roda inalterado. Este teste cobre o que passa a ser
 * possível. Mesma configuração daquele teste, para reaproveitar o contexto em cache.
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
@DisplayName("IdP externo — fase 1a: mudanças compatíveis no core")
class IdpExternoFase1aCoreTest {

    private static final String TENANT = ArchbaseSecurityTestApplication.TENANT;
    private static final String FEDERADO = "federado@fase1a.com.br";
    private static final String LOCAL = "local@fase1a.com.br";
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
    PasswordResetTokenJpaRepository resetTokenRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    ArchbaseAuthenticationService authenticationService;
    @Autowired
    ArchbaseUserService userService;
    @Autowired
    SecurityAdapter securityAdapter;
    @Autowired
    org.springframework.transaction.PlatformTransactionManager transactionManager;

    private final ObjectMapper json = new ObjectMapper();
    private UserEntity federado;

    @BeforeEach
    void preparar() {
        resetTokenRepository.deleteAll();
        apiTokenRepository.deleteAll();
        tokenRepository.deleteAll();
        userRepository.deleteAll();
        federado = userRepository.save(usuario(FEDERADO));
        userRepository.save(usuario(LOCAL));
    }

    @AfterEach
    void restaurar() {
        filtro.setExternalAuthenticators(List.of());
        filtro.setExternalPolicy(null);
        politicasDeCredencial(List.of());
        ReflectionTestUtils.setField(alvo(authenticationService), "preventUserEnumeration", false);
        SecurityContextHolder.clearContext();
    }

    // ═══════════════════════════ política agregada ═══════════════════════════

    @Nested
    @DisplayName("ExternalAuthenticationPolicy agregada")
    class PoliticaAgregada {

        @Test
        @DisplayName("sem políticas: nenhuma política efetiva (comportamento de sempre)")
        void nenhuma() {
            filtro.setExternalPolicies(List.of());
            assertThat(politicaEfetiva()).isNull();
            filtro.setExternalPolicies(null);
            assertThat(politicaEfetiva()).isNull();
        }

        @Test
        @DisplayName("uma política: é ela mesma, como quando era um bean único")
        void uma() {
            ExternalAuthenticationPolicy unica = politica(false, true);
            filtro.setExternalPolicies(List.of(unica));
            assertThat(politicaEfetiva()).isSameAs(unica);
        }

        @Test
        @DisplayName("duas: cada permissão só vale se as duas concederem")
        void duas() {
            filtro.setExternalPolicies(List.of(politica(true, true), politica(true, true)));
            assertThat(politicaEfetiva().acceptLegacyJwt()).isTrue();
            assertThat(politicaEfetiva().allowDefaultTenant()).isTrue();

            filtro.setExternalPolicies(List.of(politica(true, true), politica(false, true)));
            assertThat(politicaEfetiva().acceptLegacyJwt()).isFalse();
            assertThat(politicaEfetiva().allowDefaultTenant()).isTrue();

            filtro.setExternalPolicies(List.of(politica(true, false), politica(true, true)));
            assertThat(politicaEfetiva().acceptLegacyJwt()).isTrue();
            assertThat(politicaEfetiva().allowDefaultTenant()).isFalse();
        }
    }

    // ═══════════════════════════ ArchbaseCurrentUser ═══════════════════════════

    @Nested
    @DisplayName("ArchbaseCurrentUser e os casts que ele substitui")
    class UsuarioAtual {

        @Test
        @DisplayName("Authentication do login local / token de API: devolve o UserEntity")
        void loginLocal() {
            var auth = new UsernamePasswordAuthenticationToken(federado, null, federado.getAuthorities());
            assertThat(ArchbaseCurrentUser.of(auth)).containsSame(federado);
        }

        @Test
        @DisplayName("autenticação sem usuário local, principal comum ou null: vazio")
        void semUsuarioLocal() {
            assertThat(ArchbaseCurrentUser.of(new TestingAuthenticationToken("sessao-externa", null))).isEmpty();
            Principal qualquer = () -> "alguem";
            assertThat(ArchbaseCurrentUser.of(qualquer)).isEmpty();
            assertThat(ArchbaseCurrentUser.of(null)).isEmpty();
        }

        @Test
        @DisplayName("SecurityAdapter.getLoggedUser com autenticação externa: 'não encontrado', não ClassCastException")
        void securityAdapterExterno() {
            var externa = new TestingAuthenticationToken("sessao-externa", null, "ROLE_X");
            SecurityContextHolder.getContext().setAuthentication(externa);

            assertThatThrownBy(() -> securityAdapter.getLoggedUser())
                    .isInstanceOf(ArchbaseValidationException.class);
        }

        @Test
        @DisplayName("SecurityAdapter.getLoggedUser com login local: o mesmo usuário de antes")
        void securityAdapterLocal() {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(federado, null, federado.getAuthorities()));

            // Dentro de transação, como numa requisição real: toDomain() lê coleções lazy do usuário.
            String email = new org.springframework.transaction.support.TransactionTemplate(transactionManager)
                    .execute(status -> securityAdapter.getLoggedUser().getEmail());
            assertThat(email).isEqualTo(FEDERADO);
        }

        @Test
        @DisplayName("troca de senha com autenticação externa: IllegalStateException, não ClassCastException")
        void trocaDeSenhaExterna() {
            var externa = new TestingAuthenticationToken("sessao-externa", null, "ROLE_X");

            assertThatThrownBy(() -> userService.changePassword(pedido(SENHA, "nova-senha-1a"), externa))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ═══════════════════════════ ArchbaseLocalCredentialPolicy ═══════════════════════════

    @Nested
    @DisplayName("ArchbaseLocalCredentialPolicy (senha local desligada para a conta federada)")
    class SenhaLocal {

        @BeforeEach
        void desligarSenhaLocalDoFederado() {
            politicasDeCredencial(List.of(user -> !FEDERADO.equals(user.getEmail())));
        }

        @Test
        @DisplayName("login com a senha CERTA recebe exatamente a mesma resposta de senha errada")
        void loginIgualASenhaErrada() throws Exception {
            MockHttpServletResponse negado = login(FEDERADO, SENHA);
            MockHttpServletResponse senhaErrada = login(FEDERADO, "senha-errada");

            assertThat(negado.getStatus()).isEqualTo(401);
            assertThat(negado.getStatus()).isEqualTo(senhaErrada.getStatus());
            assertThat(negado.getContentAsString()).isEqualTo(senhaErrada.getContentAsString());
        }

        @Test
        @DisplayName("conta não coberta pela política continua entrando com senha")
        void outraContaSegue() throws Exception {
            assertThat(login(LOCAL, SENHA).getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("troca da própria senha é recusada e a senha não muda")
        void trocaRecusada() {
            var auth = new UsernamePasswordAuthenticationToken(federado, null, federado.getAuthorities());

            assertThatThrownBy(() -> userService.changePassword(pedido(SENHA, "nova-senha-1a"), auth))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("outro provedor");
            assertThat(senhaAtualE(FEDERADO, SENHA)).isTrue();
        }

        @Test
        @DisplayName("pedido de reset: mesma resposta de quem não pode trocar a senha (sem a proteção de enumeração)")
        void pedidoDeResetSemProtecao() {
            assertThatThrownBy(() -> authenticationService.sendResetPasswordEmail(FEDERADO))
                    .isInstanceOf(ArchbaseValidationException.class)
                    .hasMessageContaining("não possui autorização para alterar a senha");
            assertThat(resetTokenRepository.count()).isZero();
        }

        @Test
        @DisplayName("pedido de reset com prevent-user-enumeration: resposta uniforme, nenhum token gerado")
        void pedidoDeResetComProtecao() {
            ReflectionTestUtils.setField(alvo(authenticationService), "preventUserEnumeration", true);

            authenticationService.sendResetPasswordEmail(FEDERADO);

            assertThat(resetTokenRepository.count()).isZero();
        }

        @Test
        @DisplayName("conclusão de reset com token emitido antes do desligamento é recusada")
        void conclusaoDeResetRecusada() {
            PasswordResetTokenEntity token = PasswordResetTokenEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .token("12345678")
                    .revoked(false)
                    .expirationTime(60L)
                    .user(federado)
                    .build();
            token.setCreateEntityDate(LocalDateTime.now());
            resetTokenRepository.save(token);

            PasswordResetRequest pedido = new PasswordResetRequest();
            pedido.setEmail(FEDERADO);
            pedido.setPasswordResetToken("12345678");
            pedido.setNewPassword("nova-senha-1a");

            assertThatThrownBy(() -> authenticationService.resetPassword(pedido))
                    .isInstanceOf(ArchbaseValidationException.class)
                    .hasMessageContaining("outro provedor");
            assertThat(senhaAtualE(FEDERADO, SENHA)).isTrue();
        }

        @Test
        @DisplayName("política que lança exceção conta como negação")
        void politicaQueFalhaNega() throws Exception {
            politicasDeCredencial(List.of(user -> {
                throw new IllegalStateException("cadastro de vínculos fora do ar");
            }));

            assertThat(login(LOCAL, SENHA).getStatus()).isEqualTo(401);
        }

        @Test
        @DisplayName("com várias políticas, basta uma negar")
        void variasPoliticas() throws Exception {
            politicasDeCredencial(List.of(user -> true, user -> !LOCAL.equals(user.getEmail())));

            assertThat(login(LOCAL, SENHA).getStatus()).isEqualTo(401);
            assertThat(login(FEDERADO, SENHA).getStatus()).isEqualTo(200);
        }
    }

    // ═══════════════════════════ apoio ═══════════════════════════

    private ExternalAuthenticationPolicy politicaEfetiva() {
        return (ExternalAuthenticationPolicy) ReflectionTestUtils.getField(filtro, "externalPolicy");
    }

    /** Troca a lista nos dois serviços que a consultam (o alvo real, não o proxy transacional). */
    private void politicasDeCredencial(List<ArchbaseLocalCredentialPolicy> politicas) {
        ReflectionTestUtils.setField(alvo(authenticationService), "localCredentialPolicies", politicas);
        ReflectionTestUtils.setField(alvo(userService), "localCredentialPolicies", politicas);
    }

    private static Object alvo(Object bean) {
        return AopTestUtils.getUltimateTargetObject(bean);
    }

    private MockHttpServletResponse login(String email, String senha) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", senha))))
                .andReturn().getResponse();
    }

    private boolean senhaAtualE(String email, String senha) {
        return passwordEncoder.matches(senha, userRepository.findByEmail(email).orElseThrow().getPassword());
    }

    private static ChangePasswordRequest pedido(String atual, String nova) {
        return ChangePasswordRequest.builder().currentPassword(atual).newPassword(nova).confirmationPassword(nova).build();
    }

    private UserEntity usuario(String email) {
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID().toString())
                .createEntityDate(LocalDateTime.now())
                .name(email).description(email)
                .email(email).userName(email)
                .password(passwordEncoder.encode(SENHA))
                .isAdministrator(false).accountDeactivated(false).accountLocked(false)
                .changePasswordOnNextLogin(false).passwordNeverExpires(true).allowPasswordChange(true)
                .allowMultipleLogins(true).unlimitedAccessHours(true)
                .build();
        user.setTenantId(TENANT);
        return user;
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
}
