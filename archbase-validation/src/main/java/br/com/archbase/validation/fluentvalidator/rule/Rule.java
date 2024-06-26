package br.com.archbase.validation.fluentvalidator.rule;

public interface Rule<T> {

  default boolean apply(final T instance) {
    return true;
  }

  /**
   *
   * @param instance
   * @param value
   * @return
   */
  default boolean apply(final Object instance, final T value) {
    return apply(value);
  }

  /**
   *
   * @param instance
   * @return
   */
  default boolean support(final T instance) {
    return true;
  }

}
