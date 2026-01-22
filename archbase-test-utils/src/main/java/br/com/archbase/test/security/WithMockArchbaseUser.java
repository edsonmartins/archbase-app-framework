package br.com.archbase.test.security;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation para testes que requerem um usuário autenticado mockado.
 * Simplifica o uso de {@link org.springframework.security.test.context.support.WithMockUser}
 * para o contexto do Archbase.
 * <p>
 * Uso:
 * <pre>
 * {@code
 * @Test
 * @WithMockArchbaseUser(username = "test@example.com", roles = {"ADMIN"})
 * void testWithAuthenticatedUser() {
 *     // teste
 * }
 * }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@WithSecurityContext(factory = WithMockArchbaseUserFactory.class)
public @interface WithMockArchbaseUser {

    /**
     * Username do usuário mockado.
     * Default: "test@example.com"
     */
    String username() default "test@example.com";

    /**
     * Password do usuário mockado (não usado na autenticação real).
     * Default: "password"
     */
    String password() default "password";

    /**
     * Roles do usuário mockado.
     * Default: {"USER"}
     */
    String[] roles() default {"USER"};

    /**
     * ID do tenant para multi-tenancy.
     * Default: "test-tenant"
     */
    String tenantId() default "test-tenant";

    /**
     * ID da empresa (se aplicável).
     */
    String companyId() default "";

    /**
     * ID do projeto (se aplicável).
     */
    String projectId() default "";
}
