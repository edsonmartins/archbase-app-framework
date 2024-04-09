package br.com.archbase.validation.fluentvalidator.rule;

import java.util.function.Function;
import java.util.function.Predicate;
import br.com.archbase.validation.fluentvalidator.ArchbaseValidator;
import br.com.archbase.validation.fluentvalidator.exception.ArchbaseValidationException;
import br.com.archbase.validation.fluentvalidator.handler.ArchbaseHandlerInvalidField;

interface ValidationRule<T, P> extends Rule<P> {

  void when(final Predicate<P> when);

  void must(final Predicate<P> must);

  void withFieldName(final Function<?, String> fieldName);

  void withMessage(final Function<?, String> message);

  void withCode(final Function<?, String> code);

  void withAttemptedValue(final Function<?, Object> attemptedValue);

  void withHandlerInvalidField(final ArchbaseHandlerInvalidField<P> handleInvalid);

  void critical();

  void critical(final Class<? extends ArchbaseValidationException> clazz);

  void whenever(final Predicate<P> whenever);

  void withValidator(final ArchbaseValidator<T> validator);

}
