package br.com.archbase.event.driven.spec.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface CommandHandler {

    /**
     * Namespace do comando tratado, conforme {@code DomainCommand#namespace()}. Opcional: a assinatura do método
     * normalmente já identifica o comando.
     *
     * @since 3.4.6
     */
    String namespace() default "";

    /**
     * Nome do comando tratado, conforme {@code DomainCommand#name()}.
     *
     * @since 3.4.6
     */
    String name() default "";
}
