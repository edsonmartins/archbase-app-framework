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
}
