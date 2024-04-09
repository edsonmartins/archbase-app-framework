package br.com.archbase.validation.fluentvalidator.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import br.com.archbase.validation.fluentvalidator.exception.ArchbaseValidationException;

public final class ArchbaseValidationResult {

  private final boolean valid;

  private final Collection<Error> errors;

  /**
   *
   * @return
   */
  public static ArchbaseValidationResult ok() {
    return new ArchbaseValidationResult(true, new ArrayList<>());
  }

  /**
   *
   * @param messages
   * @return
   */
  public static ArchbaseValidationResult fail(final Collection<Error> messages) {
    return new ArchbaseValidationResult(false, Optional.ofNullable(messages).orElse(new ArrayList<>()));
  }

  private ArchbaseValidationResult(final boolean valid, final Collection<Error> messages) {
    this.valid = valid;
    errors = Collections.unmodifiableCollection(messages);
  }

  /**
   *
   * @param clazz
   */
  public <T extends ArchbaseValidationException> void isInvalidThrow(final Class<T> clazz) {
    if (!isValid()) {
      throw ArchbaseValidationException.create(clazz, this);
    }
  }

  /**
   *
   * @return
   */
  public boolean isValid() {
    return valid;
  }

  /**
   *
   * @return
   */
  public Collection<Error> getErrors() {
    return errors;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append("ValidationResult [valid=");
    builder.append(valid);
    builder.append(", ");
    builder.append("errors=");
    builder.append(errors);
    builder.append("]");
    return builder.toString();
  }

}
