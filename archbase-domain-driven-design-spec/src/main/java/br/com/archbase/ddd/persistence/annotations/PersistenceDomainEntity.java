package br.com.archbase.ddd.persistence.annotations;

import br.com.archbase.ddd.domain.contracts.Identifiable;

import java.lang.annotation.*;

/**
 * Marca uma classe como espelho de persistência para um entidade de dominio.
 *
 * @author edsonmartins
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@Documented
public @interface PersistenceDomainEntity {

    Class<? extends Identifiable> value();
}
