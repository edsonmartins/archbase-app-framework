package br.com.archbase.validation.fluentvalidator.rule;

import java.util.function.Function;

import br.com.archbase.validation.fluentvalidator.builder.AttemptedValue;
import br.com.archbase.validation.fluentvalidator.builder.Code;
import br.com.archbase.validation.fluentvalidator.builder.Critical;
import br.com.archbase.validation.fluentvalidator.builder.FieldName;
import br.com.archbase.validation.fluentvalidator.builder.HandleInvalidField;
import br.com.archbase.validation.fluentvalidator.builder.Message;
import br.com.archbase.validation.fluentvalidator.builder.Must;
import br.com.archbase.validation.fluentvalidator.builder.When;
import br.com.archbase.validation.fluentvalidator.builder.Whenever;
import br.com.archbase.validation.fluentvalidator.builder.WithValidator;

abstract class AbstractRuleBuilder<T, P, W extends When<T, P, W, N>, N extends Whenever<T, P, W, N>>
    implements Must<T, P, W, N>, Message<T, P, W, N>, FieldName<T, P, W, N>, Code<T, P, W, N>, Critical<T, P, W, N>, WithValidator<T, P, W, N>, HandleInvalidField<T, P, W, N>, AttemptedValue<T, P, W, N>, Rule<T> {

  protected final Function<T, String> fieldName;

  protected final Function<T, P> function;

  protected AbstractRuleBuilder(final Function<T, String> fieldName, final Function<T, P> function) {
    this.fieldName = fieldName;
    this.function = function;
  }

  protected AbstractRuleBuilder(final String fieldName, final Function<T, P> function) {
    this(obj -> fieldName, function);
  }

  protected AbstractRuleBuilder(final Function<T, P> function) {
    this(obj -> null, function);
  }

}
