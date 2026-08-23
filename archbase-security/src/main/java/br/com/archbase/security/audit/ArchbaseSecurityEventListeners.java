package br.com.archbase.security.audit;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Liga os eventos que o Spring Security já publica à trilha.
 *
 * <p><b>Por que ouvir em vez de chamar.</b> O serviço de autenticação tem seis pontos de retorno —
 * login, login flexível, social, MFA, refresh — e espalhar chamadas por todos garantiria que algum
 * caminho novo nascesse sem registro. Ouvir o evento cobre todos de uma vez, inclusive os que ainda
 * não existem, e mantém a auditoria fora do fluxo de autenticação.
 *
 * <p>Fica de fora o refresh de token: ele passa pelo mesmo funil de resposta, mas renovar sessão não
 * é entrar no sistema. Registrá-lo encheria a trilha de linhas que ninguém procura e afastaria os
 * logins de verdade.
 */
@Component
public class ArchbaseSecurityEventListeners {

    private final ArchbaseSecurityEventLogger logger;

    public ArchbaseSecurityEventListeners(ArchbaseSecurityEventLogger logger) {
        this.logger = logger;
    }

    @EventListener
    public void aoAutenticar(AuthenticationSuccessEvent evento) {
        logger.loginBemSucedido(nomeDe(evento.getAuthentication()));
    }

    /**
     * Cobre toda a família de falhas — senha errada, conta desativada, credencial expirada — porque
     * o evento é abstrato e o Spring publica a subclasse específica.
     *
     * <p>O nome da exceção vai como motivo: é o que distingue "errou a senha" de "a conta está
     * bloqueada", e essa distinção é a diferença entre um usuário confuso e um ataque.
     */
    @EventListener
    public void aoFalhar(AbstractAuthenticationFailureEvent evento) {
        String motivo = evento.getException() == null
                ? null
                : evento.getException().getClass().getSimpleName();
        logger.loginFalhou(nomeDe(evento.getAuthentication()), motivo);
    }

    /**
     * O identificador tentado, mesmo quando não corresponde a ninguém.
     *
     * <p>Numa tentativa com usuário inexistente é justamente esse valor que interessa: é ele que
     * mostra alguém varrendo e-mails.
     */
    private String nomeDe(Authentication autenticacao) {
        if (autenticacao == null) {
            return null;
        }
        return autenticacao.getName();
    }
}
