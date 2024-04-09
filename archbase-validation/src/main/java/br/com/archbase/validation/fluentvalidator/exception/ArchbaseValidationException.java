package br.com.archbase.validation.fluentvalidator.exception;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import br.com.archbase.validation.fluentvalidator.context.ArchbaseValidationContext;
import br.com.archbase.validation.fluentvalidator.context.ArchbaseValidationResult;

public abstract class ArchbaseValidationException extends RuntimeException {

  private static final long serialVersionUID = 2274879814700248645L;

  private final transient ArchbaseValidationResult validationResult;

  protected ArchbaseValidationException(final ArchbaseValidationResult validationResult) {
    super(validationResult.toString());
    this.validationResult = validationResult;
  }

  public ArchbaseValidationException(String message, ArchbaseValidationResult validationResult) {
    super(message);
    this.validationResult = validationResult;
  }

  public ArchbaseValidationException(Throwable cause) {
    super(cause);
    this.validationResult = ArchbaseValidationResult.ok();
  }

  public ArchbaseValidationException(String message, Throwable cause, ArchbaseValidationResult validationResult) {
    super(message, cause);
    this.validationResult = validationResult;
  }

  public ArchbaseValidationException(Throwable cause, ArchbaseValidationResult validationResult) {
    super(cause);
    this.validationResult = validationResult;
  }

  public ArchbaseValidationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, ArchbaseValidationResult validationResult) {
    super(message, cause, enableSuppression, writableStackTrace);
    this.validationResult = validationResult;
  }

  /**
   *
   * @return
   */
  public ArchbaseValidationResult getValidationResult() {
    return validationResult;
  }

  /**
   *
   * @param exceptionClass
   * @return
   */
  public static <T extends ArchbaseValidationException> RuntimeException create(final Class<T> exceptionClass) {
    return create(exceptionClass, ArchbaseValidationContext.get().getValidationResult());
  }

  /**
   *
   * @param exceptionClass
   * @param validationResult
   * @return
   */
  public static <T extends ArchbaseValidationException> RuntimeException create(final Class<T> exceptionClass, final ArchbaseValidationResult validationResult) {
    try {
      final Constructor<? extends ArchbaseValidationException> ctor =
          exceptionClass.getConstructor(ArchbaseValidationResult.class);
      return ctor.newInstance(validationResult);
    } catch (final NoSuchMethodException | SecurityException | InstantiationException
        | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      return new RuntimeException(
          "Constructor in class not found (ValidationResult validationResult)", e);
    }
  }

}
