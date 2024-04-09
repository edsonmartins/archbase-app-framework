package br.com.archbase.validation.fluentvalidator.builder;

import br.com.archbase.validation.fluentvalidator.ArchbaseValidator;

public interface WheneverProperty<T, P> extends Whenever<T, P, WhenProperty<T, P>, WheneverProperty<T, P>> {

  /**
   *
   * @param validator
   * @return
   */
  WithValidator<T, P, WhenProperty<T, P>, WheneverProperty<T, P>> withValidator(final ArchbaseValidator<P> validator);

}
