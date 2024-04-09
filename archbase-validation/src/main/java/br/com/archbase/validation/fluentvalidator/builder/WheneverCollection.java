package br.com.archbase.validation.fluentvalidator.builder;

import java.util.Collection;

import br.com.archbase.validation.fluentvalidator.ArchbaseValidator;

public interface WheneverCollection<T, P> extends Whenever<T, Collection<P>, WhenCollection<T, P>, WheneverCollection<T, P>> {

  /**
   *
   * @param validator
   * @return
   */
  WithValidator<T, Collection<P>, WhenCollection<T, P>, WheneverCollection<T, P>> withValidator(final ArchbaseValidator<P> validator);

}
