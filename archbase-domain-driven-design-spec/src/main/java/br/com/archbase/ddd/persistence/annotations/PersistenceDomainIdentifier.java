package br.com.archbase.ddd.persistence.annotations;


import br.com.archbase.ddd.domain.contracts.Identifier;

import java.lang.annotation.*;


/**
 * Marca uma classe como identificador de persistência para um entidade de dominio.
 *
 * @author edsonmartins
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@Documented
public @interface PersistenceDomainIdentifier {

    Class<? extends Identifier> value();

}
