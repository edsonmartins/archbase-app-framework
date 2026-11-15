package br.com.archbase.ddd.domain.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Identifica uma associação entre duas entidades dentro do modelo.
 * <p>
 * Sem {@code @Target} de propósito: a anotação nasceu sem alvo declarado (válida em qualquer declaração)
 * e restringir agora quebraria quem já a usa em campo, método ou tipo.
 *
 * @author edsonmartins
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface DomainAssociation {
}
