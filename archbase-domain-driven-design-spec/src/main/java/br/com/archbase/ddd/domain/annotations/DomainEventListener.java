package br.com.archbase.ddd.domain.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DomainEventListener {
    boolean asynchronous() default false;

    /**
     * Namespace do evento tratado, conforme {@link DomainEvent#namespace()}. Opcional: a assinatura do método
     * normalmente já identifica o evento. Use {@code "*"} para indicar qualquer namespace.
     *
     * @since 3.4.6
     */
    String namespace() default "";

    /**
     * Nome do evento tratado, conforme {@link DomainEvent#name()}. Use {@code "*"} para todos os eventos do
     * {@link #namespace()}.
     *
     * @since 3.4.6
     */
    String name() default "";
}
