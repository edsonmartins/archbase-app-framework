package br.com.archbase.security.auth;

import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.UserJpaRepository;
import br.com.archbase.security.service.ArchbaseAuthenticationService;
import br.com.archbase.security.service.ArchbaseEmailService;
import br.com.archbase.validation.exception.ArchbaseValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * O pedido de reset de senha não pode distinguir quem tem conta de quem não tem.
 *
 * <p><b>O que motivou.</b> A flag {@code archbase.security.prevent-user-enumeration} uniformizava
 * apenas o caminho do e-mail não cadastrado. Exercitando a proteção num projeto real, apareceram
 * outros dois caminhos que só um e-mail CADASTRADO alcança e que, por isso, denunciavam a
 * existência da conta mesmo com a proteção ligada:
 *
 * <ul>
 *   <li>usuário sem autorização para trocar a senha — respondia 400 com a mensagem explicando;</li>
 *   <li>falha ao enviar o e-mail — respondia 500, enquanto o inexistente respondia 200.</li>
 * </ul>
 *
 * <p>O segundo é o mais traiçoeiro: o vazamento não vinha da lógica, e sim da falha, de modo que
 * aparecia exatamente quando a infraestrutura de e-mail estava quebrada — e a proteção seguia
 * ligada, dando a impressão de proteger. Foi assim que um boilerplate sem
 * {@code ArchbaseEmailService} implementado permitia enumerar usuários com a flag em {@code true}.
 *
 * <p>Cada caso abaixo fixa um desses caminhos. Com a flag desligada, o comportamento anterior é
 * preservado — é o que o último caso verifica.
 */
@DisplayName("Reset de senha e enumeração de usuários")
class ResetDeSenhaNaoEnumeraUsuariosTest {

    private static final String EMAIL = "alguem@exemplo.com";

    private UserJpaRepository repository;
    private ArchbaseEmailService emailService;
    private ArchbaseAuthenticationService service;

    @BeforeEach
    void preparar() {
        repository = mock(UserJpaRepository.class);
        emailService = mock(ArchbaseEmailService.class);

        service = mock(ArchbaseAuthenticationService.class, org.mockito.Mockito.CALLS_REAL_METHODS);

        // O serviço tem doze colaboradores e só dois importam aqui. Os demais recebem mocks inertes:
        // o caminho do reset passa por revogação de token e geração de token antes de chegar ao
        // envio, e qualquer um deles nulo produziria NullPointerException — que o teste não deve
        // confundir com o comportamento que está verificando. Preenchidos por reflexão, e não pelo
        // construtor, para o teste não quebrar quando um colaborador novo entrar na lista.
        for (Field campo : ArchbaseAuthenticationService.class.getDeclaredFields()) {
            if (campo.getType().isPrimitive() || Modifier.isStatic(campo.getModifiers())) {
                continue;
            }
            if (ReflectionTestUtils.getField(service, campo.getName()) == null) {
                ReflectionTestUtils.setField(service, campo.getName(), mock(campo.getType()));
            }
        }

        ReflectionTestUtils.setField(service, "repository", repository);
        ReflectionTestUtils.setField(service, "archbaseEmailService", emailService);
    }

    private void comProtecao(boolean ligada) {
        ReflectionTestUtils.setField(service, "preventUserEnumeration", ligada);
    }

    private UserEntity usuario(boolean podeTrocarSenha) {
        UserEntity user = new UserEntity();
        user.setId("11111111-1111-1111-1111-111111111111");
        user.setEmail(EMAIL);
        user.setUserName("alguem");
        user.setName("Alguém");
        user.setAllowPasswordChange(podeTrocarSenha);
        return user;
    }

    @Test
    @DisplayName("com a proteção ligada, falha no envio do e-mail não vira erro para quem chamou")
    void falhaDeEnvioNaoVaza() {
        comProtecao(true);
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(true)));
        // É o que acontece quando a aplicação não implementou o SPI ArchbaseEmailService, ou quando
        // o SMTP está fora do ar.
        doThrow(new UnsupportedOperationException("forneça uma implementação de ArchbaseEmailService"))
                .when(emailService).sendResetPasswordEmail(anyString(), anyString(), anyString(), anyString());

        assertThatCode(() -> service.sendResetPasswordEmail(EMAIL))
                .as("e-mail cadastrado com envio quebrado responde como qualquer outro")
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("com a proteção ligada, usuário sem autorização de troca não vira erro")
    void semAutorizacaoNaoVaza() {
        comProtecao(true);
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(false)));

        assertThatCode(() -> service.sendResetPasswordEmail(EMAIL))
                .as("resposta igual à de um e-mail não cadastrado")
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("com a proteção ligada, e-mail não cadastrado segue silencioso")
    void inexistenteNaoVaza() {
        comProtecao(true);
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatCode(() -> service.sendResetPasswordEmail(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("com a proteção desligada, o comportamento anterior é preservado")
    void semProtecaoMantemOContratoAntigo() {
        comProtecao(false);

        when(repository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.sendResetPasswordEmail(EMAIL))
                .as("e-mail não cadastrado continua respondendo 'não encontrado'")
                .isInstanceOf(ArchbaseValidationException.class);

        when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(false)));
        assertThatThrownBy(() -> service.sendResetPasswordEmail(EMAIL))
                .as("usuário sem autorização continua respondendo o motivo")
                .isInstanceOf(ArchbaseValidationException.class);
    }
}
