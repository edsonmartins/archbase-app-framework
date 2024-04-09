package br.com.archbase.validation.fluentvalidator.aspect;

import br.com.archbase.validation.fluentvalidator.annotation.ArchbaseCleanValidationContextException;
import br.com.archbase.validation.fluentvalidator.context.ArchbaseValidationContext;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class ArchbaseValidationExceptionAdvice {

  @AfterThrowing("execution(* *(..)) && @annotation(cleanValidationContextException)")
  public void afterThrowing(final ArchbaseCleanValidationContextException archbaseCleanValidationContextException) {
    ArchbaseValidationContext.remove();
  }

}
