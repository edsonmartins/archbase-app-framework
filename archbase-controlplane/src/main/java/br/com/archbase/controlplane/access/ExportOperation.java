package br.com.archbase.controlplane.access;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca um endpoint que não é {@code GET} mas só lê dados — exportação, relatório por {@code POST} com filtro no corpo.
 * Ele continua liberado no modo restrito (licença vencida além da carência ou tenant suspenso), que de resto recusa
 * todo método que não seja {@code GET}, {@code HEAD} ou {@code OPTIONS}.
 *
 * @since 3.4.6
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ExportOperation {
}
