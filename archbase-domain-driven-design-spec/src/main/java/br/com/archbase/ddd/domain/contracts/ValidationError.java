package br.com.archbase.ddd.domain.contracts;

public class ValidationError {

  private final String message;

  private final String field;

  private final Object attemptedValue;

  private final String code;

  /**
   *
   * @param field
   * @param message
   * @param code
   * @param attemptedValue
   * @return
   */
  public static ValidationError create(final String field, final String message, final String code, final Object attemptedValue) {
    return new ValidationError(field, message, code, attemptedValue);
  }

  protected ValidationError(final String field, final String message, final String code, final Object attemptedValue) {
    this.field = field;
    this.message = message;
    this.code = code;
    this.attemptedValue = attemptedValue;
  }

  /**
   *
   * @return
   */
  public String getField() {
    return field;
  }

  /**
   *
   * @return
   */
  public String getMessage() {
    return message;
  }

  /**
   *
   * @return
   */
  public String getCode() {
    return code;
  }

  /**
   *
   * @return
   */
  public Object getAttemptedValue() {
    return attemptedValue;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();

    builder.append("Error [");
    builder.append("message=");
    builder.append(message);
    builder.append(", ");
    builder.append("field=");
    builder.append(field);
    builder.append(", ");
    builder.append("attemptedValue=");
    builder.append(attemptedValue);
    builder.append(", ");
    builder.append("code=");
    builder.append(code);
    builder.append("]");

    return builder.toString();
  }

}
