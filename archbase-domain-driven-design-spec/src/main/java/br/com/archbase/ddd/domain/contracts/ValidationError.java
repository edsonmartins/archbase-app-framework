package br.com.archbase.ddd.domain.contracts;

import java.math.BigDecimal;
import java.math.BigInteger;

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

  /**
   * Verifica se o valor é de um tipo simples (String, Integer, Double, etc.)
   * ou um tipo primitivo.
   */
  private boolean isSimpleValue(Object value) {
    return value == null || value instanceof String || value instanceof Number ||
            value instanceof Boolean || value instanceof Character;
  }

  protected ValidationError(final String field, final String message, final String code, final Object attemptedValue) {
    this.field = field;
    this.message = message;
    this.code = code;
    this.attemptedValue = isSimpleValue(attemptedValue)?attemptedValue:null;
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
