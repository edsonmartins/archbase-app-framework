package br.com.archbase.ddd.domain.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca um modelo de leitura (CQRS): projeção otimizada para consulta, alimentada por eventos ou comandos e que não
 * é alterada diretamente pelo domínio. Portado do jMolecules ({@code org.jmolecules.architecture.cqrs.QueryModel}).
 *
 * @since 3.4.6
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DomainQueryModel {
}
