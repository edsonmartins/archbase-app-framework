package br.com.archbase.validation.fluentvalidator.rule;

import java.util.Collection;

class RuleProcessorFailFast implements RuleProcessorStrategy {

  @Override
  public <E> boolean process(final Object obj, final E value, final Collection<Rule<E>> rules) {
    return rules.stream().allMatch(rule -> this.process(obj, value, rule));
  }

  @Override
  public <E> boolean process(final E value, final Collection<Rule<E>> rules) {
    return rules.stream().allMatch(rule -> this.process(value, rule));
  }

}
