package br.com.archbase.ddd.domain.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca o campo ou método que carrega a identidade de uma entidade ou agregado anotado (modelo baseado em anotações,
 * em que a classe não implementa {@link br.com.archbase.ddd.domain.contracts.Identifiable}).
 * <p>
 * Complementa {@link DomainIdentifier}, que marca o <em>tipo</em> do identificador. Portado do jMolecules
 * ({@code org.jmolecules.ddd.annotation.Identity}).
 *
 * @since 3.4.6
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface DomainIdentity {
}
