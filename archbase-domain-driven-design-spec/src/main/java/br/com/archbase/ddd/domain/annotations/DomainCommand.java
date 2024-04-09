package br.com.archbase.ddd.domain.annotations;


import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface DomainCommand {
}
