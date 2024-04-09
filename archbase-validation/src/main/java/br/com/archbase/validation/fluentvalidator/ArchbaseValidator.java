package br.com.archbase.validation.fluentvalidator;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import br.com.archbase.validation.fluentvalidator.builder.RuleBuilderCollection;
import br.com.archbase.validation.fluentvalidator.builder.RuleBuilderProperty;
import br.com.archbase.validation.fluentvalidator.context.ArchbaseValidationResult;
import br.com.archbase.validation.fluentvalidator.rule.Rule;
import br.com.archbase.validation.fluentvalidator.transform.ValidationResultTransform;

public interface ArchbaseValidator<T> extends Rule<T> {

  /**
   *
   */
  void rules();

  /**
   *
   */
  void failFastRule();

  /**
   *
   * @return Current count element on collection
   */
  Integer getCounter();

  /**
   *
   * @param property
   */
  void setPropertyOnContext(final String property);

  /**
   *
   * @param property
   * @param clazz
   * @return
   */
  <P> P getPropertyOnContext(final String property, final Class<P> clazz);

  /**
   *
   * @param instance
   * @return
   */
  ArchbaseValidationResult validate(final T instance);

  /**
   *
   * @param instance
   * @param transform
   * @return
   */
  <E> E validate(final T instance, final ValidationResultTransform<E> transform);

  /**
   *
   * @param instances
   * @return
   */
  List<ArchbaseValidationResult> validate(final Collection<T> instances);

  /**
   *
   * @param instances
   * @param transform
   * @return
   */
  <E> List<E> validate(final Collection<T> instances, final ValidationResultTransform<E> transform);

  /**
   *
   * @param <P>
   * @param function
   * @return
   */
  <P> RuleBuilderProperty<T, P> ruleFor(final Function<T, P> function);

  /**
   *
   * @param <P>
   * @param fieldName
   * @param function
   * @return
   */
  <P> RuleBuilderProperty<T, P> ruleFor(final String fieldName, final Function<T, P> function);

  /**
   *
   * @param <P>
   * @param function
   * @return
   */
  <P> RuleBuilderCollection<T, P> ruleForEach(final Function<T, Collection<P>> function);

  /**
   *
   * @param <P>
   * @param fieldName
   * @param function
   * @return
   */
  <P> RuleBuilderCollection<T, P> ruleForEach(final String fieldName, final Function<T, Collection<P>> function);
}
