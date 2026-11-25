package br.com.archbase.error.handling;

import br.com.archbase.error.handling.handler.SpringSecurityArchbaseApiExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Exceções do Spring Security: classe mapeada e subclasses")
class SpringSecurityArchbaseApiExceptionHandlerTest {

    private final ArchbaseErrorHandlingProperties properties = new ArchbaseErrorHandlingProperties();
    private final SpringSecurityArchbaseApiExceptionHandler handler =
            new SpringSecurityArchbaseApiExceptionHandler(properties);

    @Test
    @DisplayName("AuthorizationDeniedException (negação da segurança de método) vira 403, com o código da AccessDeniedException")
    void negacaoDeMetodo() {
        properties.getCodes().put(AccessDeniedException.class.getName(), "ACCESS_DENIED");
        var negada = new AuthorizationDeniedException("Access Denied", new AuthorizationDecision(false));

        assertThat(handler.canHandle(negada)).isTrue();
        ArchbaseApiErrorResponse resposta = handler.handle(negada);
        assertThat(resposta.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getCode()).isEqualTo("ACCESS_DENIED");
    }

    @Test
    @DisplayName("código próprio configurado para a subclasse continua valendo")
    void codigoProprio() {
        properties.getCodes().put(AuthorizationDeniedException.class.getName(), "SEM_PERMISSAO");
        var negada = new AuthorizationDeniedException("Access Denied", new AuthorizationDecision(false));

        assertThat(handler.handle(negada).getCode()).isEqualTo("SEM_PERMISSAO");
    }

    @Test
    @DisplayName("classes exatas mantêm o status de antes")
    void exatas() {
        assertThat(handler.handle(new AccessDeniedException("x")).getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(handler.handle(new BadCredentialsException("x")).getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("exceção fora da hierarquia mapeada não é tratada aqui")
    void foraDaHierarquia() {
        assertThat(handler.canHandle(new IllegalStateException("x"))).isFalse();
    }
}
