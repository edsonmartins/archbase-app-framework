package br.com.archbase.resource.logger.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação de nível de classe e método para ativar o registro automático de LOG.
 * Adicioná-lo à classe ou método permite registrar nele. A anotação no método
 * tem precedência sobre a da classe.
 *
 * @author edsonmartins
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Logging {
}
