package br.com.archbase.controlplane.access;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exige papel administrativo do Control Plane ({@code adm} do token). Recusa com 403 e
 * {@code {"code":"NOT_ADMIN"}}.
 * <ul>
 * <li>{@link AdminScope#TENANT}: {@code tenant_admin} ou {@code client_admin};</li>
 * <li>{@link AdminScope#CLIENT}: só {@code client_admin}.</li>
 * </ul>
 * Na classe, vale para todos os métodos; no método, prevalece sobre a da classe. Token de instância nunca passa.
 *
 * @since 3.4.6
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RequiresAdmin {

    AdminScope value() default AdminScope.TENANT;
}
