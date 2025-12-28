package br.com.archbase.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation composta para testes de integração do Archbase.
 * Combina as annotations mais comuns usadas em testes.
 * <p>
 * Uso:
 * <pre>
 * {@code
 * @ArchbaseTest
 * class MyRepositoryTest {
 *     // teste
 * }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootTest
@ActiveProfiles("test")
public @interface ArchbaseTest {

    /**
     * Alias para {@link SpringBootTest#classes}.
     */
    @AliasFor(annotation = SpringBootTest.class, attribute = "classes")
    Class<?>[] classes() default {};

    /**
     * Alias para {@link SpringBootTest#properties}.
     */
    @AliasFor(annotation = SpringBootTest.class, attribute = "properties")
    String[] properties() default {};

    /**
     * Alias para {@link ActiveProfiles#profiles}.
     * Se vazio, usa "test" como padrão.
     */
    @AliasFor(annotation = ActiveProfiles.class, attribute = "profiles")
    String[] value() default {};

    /**
     * Alias para {@link ActiveProfiles#profiles}.
     */
    @AliasFor(annotation = ActiveProfiles.class, attribute = "profiles")
    String[] profiles() default "test";

}
