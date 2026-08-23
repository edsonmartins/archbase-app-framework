package br.com.archbase.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca um endpoint do próprio módulo de segurança como <b>administrativo</b>: criar usuário,
 * conceder permissão, emitir token de API, apagar grupo.
 *
 * <p><b>Por que existe.</b> Os controllers de {@code archbase-security} não tinham nenhuma
 * anotação de autorização. Com a configuração padrão ({@code anyRequest().authenticated()}), o
 * usuário mais restrito da aplicação alcançava {@code POST /api/v1/user} com
 * {@code isAdministrator: true} e {@code POST /api/v1/resource/permissions} — escalação de
 * privilégio completa, presente em todo backend que usa o framework.
 *
 * <p>O que a marcação faz é decidido por
 * {@code archbase.security.admin-endpoints.policy}; veja
 * {@link br.com.archbase.security.config.SecurityAdminAuthorizationManager}.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ArchbaseSecurityAdminEndpoint {

    /**
     * Recurso avaliado quando a política é {@code permission}. Vazio herda o valor declarado na
     * classe.
     */
    String resource() default "";

    /** Ação avaliada quando a política é {@code permission}. */
    String action() default "MANAGE";

    /**
     * Exclui o método da checagem administrativa.
     *
     * <p>Necessário porque a marcação costuma ficar na classe, e alguns métodos desses mesmos
     * controllers são de autoatendimento — trocar a própria senha, ler as próprias permissões.
     * Bloqueá-los junto transformaria a correção numa quebra de funcionalidade legítima.
     */
    boolean selfService() default false;

    /**
     * Autoatendimento <b>condicionado ao alvo</b>: libera quando o que o método recebe identifica o
     * próprio usuário autenticado, e mantém a checagem administrativa para qualquer outro alvo.
     *
     * <p><b>O que isto conserta.</b> {@code selfService} é tudo ou nada, e há um caso frequente que
     * ele não cobre: ler o próprio cadastro. {@code GET /api/v1/user/{id}} é o mesmo método tanto
     * para "meus dados" quanto para "os dados de outra pessoa" — o primeiro é autoatendimento, o
     * segundo é administração. Sem distinguir os dois, {@code admin-only} devolvia 403 para todo
     * não-administrador que apenas abrisse a própria tela de perfil, o que torna a proteção
     * inutilizável na prática e leva a desligá-la inteira.
     *
     * <p>Vale para id, e-mail e nome de usuário, porque são as três formas pelas quais estes
     * controllers recebem a identificação de uma pessoa.
     *
     * <p><b>Só em métodos de leitura.</b> A comparação diz <i>quem</i> é o alvo, não <i>o que</i> vai
     * ser feito com ele: marcar um método que altera ou remove daria a qualquer usuário o poder de
     * alterar ou remover a si mesmo.
     */
    boolean selfServiceOnOwnIdentity() default false;
}
