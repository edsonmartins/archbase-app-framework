package br.com.archbase.ddd.domain.annotations;

import java.lang.annotation.*;

/**
 * Identifica um contexto limitado. Uma descrição de um limite (normalmente um subsistema ou o trabalho de uma equipe específica)
 * dentro do qual um determinado modelo é definido e aplicável. Um contexto limitado tem um estilo arquitetônico e contém
 * lógica de domínio e lógica técnica.
 *
 * @see <a href="https://domainlanguage.com/wp-content/uploads/2016/05/DDD_Reference_2015-03.pdf">Domain-Driven Design
 * Reference (Evans) - Bounded Contexts</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PACKAGE, ElementType.ANNOTATION_TYPE})
@Documented
public @interface DomainBoundedContext {

    /**
     * Um identificador estável para o contexto limitado. Se não for definido, um identificador será derivado do anotado
     * elemento, geralmente um pacote. Isso permite que as ferramentas derivem o nome e a descrição aplicando algum tipo de convenção
     * para o identificador.
     * <p>
     * Supondo um pacote {@code br.com.acme.app} anotado com {@code BoundedContext}, as ferramentas podem usar um recurso
     * agrupar para pesquisar as chaves {@code br.com.acme.app._name} e {@code br.com.acme.app._description} para resolver o nome e
     * descrição respectivamente.
     *
     * @return
     */
    String id() default "";


    /**
     * Um nome legível para o contexto limitado. Pode ser substituído por um mecanismo de resolução externo via
     * {@link #id()}. As ferramentas devem evitar que {@link #value()} e {@link #name()} sejam configurados ao mesmo tempo
     * Tempo. Em caso de dúvida, o valor definido em {@link #name()} será o preferido.
     *
     * @return
     * @see #id()
     */
    String name() default "";


    /**
     * Um alias para {@link #name()}. As ferramentas devem evitar que {@link #value()} e {@link #name()} sejam
     * configurados ao mesmo tempo. Em caso de dúvida, o valor definido em {@link #name()} será o preferido.
     *
     * @return
     * @see #name()
     */
    String value() default "";

    /**
     * Uma descrição legível por humanos para o contexto limitado. Pode ser substituído por um mecanismo de resolução externo via
     * {@link #id()}.
     *
     * @return
     * @see #id()
     */
    String description() default "";
}
