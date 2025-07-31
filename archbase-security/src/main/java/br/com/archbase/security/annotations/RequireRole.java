package br.com.archbase.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação para controle de acesso baseado em roles personalizadas da aplicação.
 * Esta anotação permite integrar roles específicas do domínio de negócio
 * com o sistema de segurança do Archbase.
 * 
 * Diferente de @RequireProfile (que usa profiles do Archbase), esta anotação
 * permite usar roles customizadas que podem ser mapeadas via enrichers.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    
    /**
     * Roles necessárias para acessar o recurso.
     * Valores dependem da implementação da aplicação.
     * Exemplos: "STORE_ADMIN", "CUSTOMER", "DRIVER", "PLATFORM_ADMIN"
     */
    String[] value();
    
    /**
     * Se true, require que o usuário tenha TODAS as roles listadas (AND).
     * Se false, require apenas UMA das roles listadas (OR).
     * Default: false (OR)
     */
    boolean requireAll() default false;
    
    /**
     * Se true, require que o usuário seja admin da plataforma.
     * Verificação adicional além das roles especificadas.
     */
    boolean requirePlatformAdmin() default false;
    
    /**
     * Se true, permite acesso apenas para owners (não funcionários).
     * Útil para separar proprietários de funcionários em contextos como lojas.
     */
    boolean ownerOnly() default false;
    
    /**
     * Contexto específico para validação condicional.
     * Pode ser usado pelo enricher para aplicar lógica específica.
     * Exemplos: "ACTIVE_STORE", "AVAILABLE_DRIVER"
     */
    String context() default "";
    
    /**
     * Se true, permite bypass para administradores de sistema.
     */
    boolean allowSystemAdmin() default true;
    
    /**
     * Mensagem customizada para acesso negado.
     */
    String message() default "Acesso negado para esta role";
}