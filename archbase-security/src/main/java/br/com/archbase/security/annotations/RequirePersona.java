package br.com.archbase.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação para controle de acesso baseado em Personas de negócio.
 * 
 * Permite definir quais personas têm acesso a métodos ou classes específicas,
 * integrando com enrichers da aplicação para validações de contexto.
 * 
 * Exemplos de personas: CUSTOMER, STORE_ADMIN, DRIVER, PLATFORM_ADMIN
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePersona {
    
    /**
     * Personas necessárias para acessar o recurso.
     * Exemplos: "CUSTOMER", "STORE_ADMIN", "DRIVER", "PLATFORM_ADMIN"
     */
    String[] value();
    
    /**
     * Se true, require que o usuário tenha TODAS as personas listadas (AND).
     * Se false, require apenas UMA das personas listadas (OR).
     * Default: false (OR)
     */
    boolean requireAll() default false;
    
    /**
     * Contexto específico para validação.
     * Usado pelos enrichers para aplicar lógica condicional.
     * Exemplos: "STORE_APP", "CUSTOMER_APP", "DRIVER_APP", "WEB_ADMIN"
     */
    String context() default "";
    
    /**
     * Dados de contexto adicional como JSON.
     * Permite passar parâmetros específicos para o enricher.
     * Exemplo: {"storeId": "123", "region": "SP"}
     */
    String contextData() default "";
    
    /**
     * Se true, permite bypass para administradores de sistema.
     */
    boolean allowSystemAdmin() default true;
    
    /**
     * Se true, verifica se o usuário está ativo.
     */
    boolean requireActiveUser() default true;
    
    /**
     * Se true, permite acesso apenas para owners (não funcionários).
     * Útil para separar proprietários de funcionários.
     */
    boolean ownerOnly() default false;
    
    /**
     * Resource específico para validação adicional.
     * Se especificado, também valida permissão no resource.
     */
    String resource() default "";
    
    /**
     * Action específica para validação adicional.
     */
    String action() default "";
    
    /**
     * Mensagem customizada para acesso negado.
     */
    String message() default "Acesso negado - Persona não autorizada";
}