package br.com.archbase.ddd.infraestructure.persistence.jdbc.apt;

import java.lang.annotation.*;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE})
@Documented
public @interface DefaultSchema {

    String value();
}