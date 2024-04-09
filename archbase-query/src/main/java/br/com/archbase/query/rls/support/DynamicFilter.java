package br.com.archbase.query.rls.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Esta anotação permite adicionar um filtro dinâmico para uma ou mais entidades.
 * Com isso conseguimos implementar regras/predicados para implementar o conceito
 * de filtro dinâmico e RLS - Row Level Security.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DynamicFilter {
    /**
     * Nome da classe que implementa os filtros
     *
     * @return Nome da classe que implementa {@link DynamicFilter}
     */
    Class[] value() default {};
}
