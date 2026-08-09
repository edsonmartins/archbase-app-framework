package br.com.archbase.security.config;

import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.service.ArchbaseSecurityService;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Ler o próprio cadastro não é administração.
 *
 * <p><b>O que motivou.</b> Com {@code admin-endpoints.policy=admin-only}, um usuário comum tomava 403
 * ao abrir a própria tela de perfil: {@code GET /api/v1/user/{id}} é o mesmo método tanto para "meus
 * dados" quanto para "os dados de outra pessoa", e a marcação administrativa não distinguia os dois.
 * Uma proteção que impede a pessoa de ver o próprio cadastro não é usada — é desligada.
 *
 * <p>O par de testes é o ponto: liberar o próprio cadastro só tem valor se continuar negando o dos
 * outros. Um teste sozinho passaria com um {@code return true}.
 */
class LerOProprioCadastroTest {

    private static final String MEU_ID = "f0a1b23c-a7c5-4043-a5d3-7a7dbd3cbcf1";
    private static final String MEU_EMAIL = "caroline@exemplo.test";

    private SecurityAdminAuthorizationManager manager;
    private UserEntity euMesmo;

    @BeforeEach
    void preparar() {
        manager = new SecurityAdminAuthorizationManager(mock(ArchbaseSecurityService.class));
        ReflectionTestUtils.setField(manager, "policy", "admin-only");

        euMesmo = UserEntity.builder()
                .id(MEU_ID)
                .email(MEU_EMAIL)
                .userName(MEU_EMAIL)
                .name("Caroline")
                .description("Caroline")
                .isAdministrator(false)
                .accountDeactivated(false)
                .accountLocked(false)
                .build();
    }

    @Test
    @DisplayName("não-administrador lê o próprio cadastro pelo id")
    void leOProprioCadastroPeloId() {
        assertThat(decidir("getUserById", MEU_ID)).isTrue();
    }

    @Test
    @DisplayName("não-administrador lê o próprio cadastro pelo e-mail")
    void leOProprioCadastroPeloEmail() {
        assertThat(decidir("getUserByEmail", MEU_EMAIL)).isTrue();
    }

    @Test
    @DisplayName("mas continua sem ler o cadastro de outra pessoa")
    void naoLeOCadastroDeOutro() {
        // Este é o teste que dá sentido aos dois de cima. Sem ele, a liberação poderia ter virado
        // "qualquer um lê qualquer cadastro" e a suíte seguiria verde.
        assertThat(decidir("getUserById", "de-outra-pessoa-0000-0000-000000000000")).isFalse();
        assertThat(decidir("getUserByEmail", "outra@exemplo.test")).isFalse();
    }

    @Test
    @DisplayName("método administrativo sem a marcação segue exigindo administrador")
    void metodoAdministrativoSegueFechado() {
        // createUser recebe um corpo, não um texto: mesmo que alguém envie o próprio id lá dentro,
        // não há argumento de texto para casar — e o método não é marcado, então nem é avaliado.
        assertThat(decidir("createUser", MEU_ID)).isFalse();
    }

    /** Chama o manager como o Spring chamaria, sobre o método real do controller. */
    private boolean decidir(String metodo, String argumento) {
        Method alvo = List.of(br.com.archbase.security.controller.UserController.class.getMethods())
                .stream()
                .filter(m -> m.getName().equals(metodo))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("método não encontrado: " + metodo));

        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getMethod()).thenReturn(alvo);
        when(invocation.getArguments()).thenReturn(new Object[]{argumento});
        when(invocation.getThis()).thenReturn(null);

        Authentication auth = new UsernamePasswordAuthenticationToken(euMesmo, "n/a", List.of());
        return manager.authorize(() -> auth, invocation).isGranted();
    }
}
