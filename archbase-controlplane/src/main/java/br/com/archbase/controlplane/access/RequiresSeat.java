package br.com.archbase.controlplane.access;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exige assento: o token de sessão traz {@code seat} e, se o Control Plane já informou os assentos do tenant, o
 * usuário continua entre os que têm assento (um assento retirado vale antes do token vencer). Recusa com 403 e
 * {@code {"code":"NO_SEAT"}}. Token de instância e JWT legado não têm assento e são recusados.
 *
 * @since 3.4.6
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RequiresSeat {
}
