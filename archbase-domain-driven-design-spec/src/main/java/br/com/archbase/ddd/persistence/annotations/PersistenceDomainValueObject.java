package br.com.archbase.ddd.persistence.annotations;

import br.com.archbase.ddd.domain.contracts.ValueObject;

import java.lang.annotation.*;


/**
 * Marca uma classe como objeto de valor de persistência para um entidade de dominio.
 *
 * @author edsonmartins
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@Documented
public @interface PersistenceDomainValueObject {

    Class<? extends ValueObject> value();

}
