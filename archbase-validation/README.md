# Java Fluent Validator

Validating data is a common task that occurs throughout any application, especially the business logic layer. As for some quite complex scenarios, often the same or similar validations are scattered everywhere, thus it is hard to reuse code and break the [DRY](https://en.wikipedia.org/wiki/Don%27t_repeat_yourself) rule.

_**Compatible JDK 8, 11, 15, 16 and 17**_

This library supports **`Kotlin`** aswell

## Sample

### Java

```java
import static predicate.br.com.archbase.validation.fluentvalidator.ComparablePredicate.equalTo;
import static predicate.br.com.archbase.validation.fluentvalidator.LogicalPredicate.not;
import static predicate.br.com.archbase.validation.fluentvalidator.ObjectPredicate.nullValue;
import static predicate.br.com.archbase.validation.fluentvalidator.StringPredicate.stringContains;
import static predicate.br.com.archbase.validation.fluentvalidator.StringPredicate.stringEmptyOrNull;

import br.com.archbase.validation.fluentvalidator.AbstractArchbaseValidator;
import model.br.com.archbase.validation.fluentvalidator.Boy;
import model.br.com.archbase.validation.fluentvalidator.Gender;

public class JavaValidatorBoy extends AbstractArchbaseValidator<Boy> {

    @Override
    protected void rules() {

        ruleFor(Boy::getGender)
                .must(equalTo(Gender.MALE))
                .when(not(nullValue()))
                .withMessage("gender of boy must be MALE")
                .withFieldName("gender")
                .critical();

        ruleFor(Boy::getName)
                .must(stringContains("John"))
                .when(not(stringEmptyOrNull()))
                .withMessage("child name must contains key John")
                .withFieldName("name");
    }

}
```

### Kotlin

```kotlin
import predicate.br.com.archbase.validation.fluentvalidator.ComparablePredicate.equalTo;
import predicate.br.com.archbase.validation.fluentvalidator.LogicalPredicate.not;
import predicate.br.com.archbase.validation.fluentvalidator.ObjectPredicate.nullValue;
import predicate.br.com.archbase.validation.fluentvalidator.StringPredicate.stringContains;
import predicate.br.com.archbase.validation.fluentvalidator.StringPredicate.stringEmptyOrNull;

import model.br.com.archbase.validation.fluentvalidator.Boy
import model.br.com.archbase.validation.fluentvalidator.Gender;

import br.com.archbase.validation.fluentvalidator.AbstractArchbaseValidator;

class KotlinValidatorBoy : AbstractValidator<Boy> {
	
  constructor() : super();

  override fun rules() {

    ruleFor(Boy::getGender)
      .must(equalTo(Gender.MALE))
      .`when`(not(nullValue()))
        .withMessage("gender of boy must be MALE")
        .withFieldName("gender")
        .critical();

    ruleFor(Boy::getName)
      .must(stringContains("John"))
      .`when`(not(stringEmptyOrNull()))
        .withMessage("child name must contains key John")
        .withFieldName("name");
  }

}
```

## Index

1. [Quick start](documentation/1-quick-start.md)
   - [Step by Step](documentation/2-step-by-step.md)
   - [Spring support](documentation/3-spring-support.md)
2. [Validator](documentation/4-validator-methods.md)
3. [Builder](documentation/5-builder-methods.md)
4. [Predicate](documentation/6-predicate-methods.md)
5. [ValidationResult](documentation/7-validation-methods.md)
6. [PredicateBuilder](documentation/8-predicate-builder.md)
7. [FunctionBuilder](documentation/9-function-builder.md)
8. Examples
   - [Samples](src/test/java/br/com/fluentvalidator/ValidatorTest.java)
   - [Spring support samples](src/test/java/br/com/fluentvalidator/spring/ValidatorSpringTest.java)
