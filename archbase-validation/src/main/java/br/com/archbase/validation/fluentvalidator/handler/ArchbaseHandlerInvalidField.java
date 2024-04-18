package br.com.archbase.validation.fluentvalidator.handler;

import java.util.Collection;
import java.util.Collections;
import br.com.archbase.ddd.domain.contracts.ValidationError;

public interface ArchbaseHandlerInvalidField<P> {

  default Collection<ValidationError> handle(final P attemptedValue) {
    return Collections.emptyList();
  }

  default Collection<ValidationError> handle(final Object instance, final P attemptedValue) {
    return handle(attemptedValue);
  }

}
