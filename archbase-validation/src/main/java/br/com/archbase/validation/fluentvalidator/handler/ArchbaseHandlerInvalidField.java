package br.com.archbase.validation.fluentvalidator.handler;

import java.util.Collection;
import java.util.Collections;
import br.com.archbase.validation.fluentvalidator.context.Error;

public interface ArchbaseHandlerInvalidField<P> {

  default Collection<Error> handle(final P attemptedValue) {
    return Collections.emptyList();
  }

  default Collection<Error> handle(final Object instance, final P attemptedValue) {
    return handle(attemptedValue);
  }

}
