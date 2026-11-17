package br.com.archbase.ddd.domain.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca o método que despacha um {@link DomainCommand} (tipicamente para o {@code CommandBus}). Serve para
 * documentação e ferramentas ligarem quem emite o comando a quem o trata. Portado do jMolecules
 * ({@code org.jmolecules.architecture.cqrs.CommandDispatcher}).
 *
 * @since 3.4.6
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface DomainCommandDispatcher {

    /**
     * O comando despachado, como {@code namespace.name} de {@link DomainCommand}. Vazio: a assinatura do método já
     * deixa claro.
     */
    String dispatches() default "";
}
