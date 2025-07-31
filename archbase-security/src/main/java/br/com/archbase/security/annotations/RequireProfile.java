package br.com.archbase.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação para controle de acesso baseado em Profiles do Archbase.
 * Permite definir quais profiles têm acesso a métodos ou classes específicas.
 * 
 * Esta anotação é processada pelo SecurityAspect do Archbase e integra
 * com o sistema de Permission/Action/Resource existente.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireProfile {
    
    /**
     * Nomes dos profiles necessários para acessar o recurso.
     * Exemplos: "ADMIN", "USER", "STORE_MANAGER", "PLATFORM_ADMIN"
     * 
     * Se múltiplos profiles forem especificados, o comportamento é controlado
     * pelo parâmetro 'requireAll'.
     */
    String[] value();
    
    /**
     * Se true, require que o usuário tenha TODOS os profiles listados (AND).
     * Se false, require apenas UM dos profiles listados (OR).
     * Default: false (OR)
     */
    boolean requireAll() default false;
    
    /**
     * Resource específico do Archbase para validação adicional.
     * Se especificado, verifica se o profile tem permissão neste resource.
     * Deve corresponder ao name de um Resource cadastrado no sistema.
     */
    String resource() default "";
    
    /**
     * Action específica do Archbase para validação adicional.
     * Se especificado, valida se o profile pode executar esta action no resource.
     * Deve corresponder ao name de uma Action cadastrada no sistema.
     */
    String action() default "";
    
    /**
     * Se true, permite bypass para administradores de sistema.
     * Usuários com isAdministrator=true passam na validação automaticamente.
     */
    boolean allowSystemAdmin() default true;
    
    /**
     * Se true, verifica se o usuário está ativo (não desativado/bloqueado).
     */
    boolean requireActiveUser() default true;
    
    /**
     * Mensagem customizada para acesso negado.
     */
    String message() default "Acesso negado - Profile não autorizado";
}