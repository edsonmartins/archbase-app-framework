package br.com.archbase.ddd.domain.annotations;


import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface DomainCommand {

    /**
     * Namespace do comando. Vazio: o pacote do tipo.
     *
     * @since 3.4.6
     */
    String namespace() default "";

    /**
     * Nome lógico do comando, independente do nome da classe. Vazio: o nome simples do tipo.
     *
     * @since 3.4.6
     */
    String name() default "";
}
