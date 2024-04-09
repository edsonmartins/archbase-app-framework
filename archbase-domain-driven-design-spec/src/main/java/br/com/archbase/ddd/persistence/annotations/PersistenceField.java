package br.com.archbase.ddd.persistence.annotations;


import java.lang.annotation.*;

/**
 * Marca um campo para ser persistido para um entidade de dominio.
 *
 * @author edsonmartins
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
@Documented
public @interface PersistenceField {
}
