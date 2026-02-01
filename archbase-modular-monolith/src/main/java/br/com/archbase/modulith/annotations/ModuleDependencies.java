package br.com.archbase.modulith.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container para múltiplas anotações {@link ModuleDependency}.
 *
 * @author Archbase Team
 * @since 3.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ModuleDependencies {
    ModuleDependency[] value();
}
