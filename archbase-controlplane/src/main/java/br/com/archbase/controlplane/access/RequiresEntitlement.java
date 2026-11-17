package br.com.archbase.controlplane.access;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exige que o tenant da requisição tenha o entitlement. Recusa com 403 e {@code {"code":"NOT_ENTITLED"}}.
 * Vale para qualquer requisição com tenant no contexto, inclusive as autenticadas pelo JWT legado.
 *
 * @since 3.4.6
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RequiresEntitlement {

    /** Chave do entitlement (ex.: {@code powerview.export_excel}). */
    String value();
}
