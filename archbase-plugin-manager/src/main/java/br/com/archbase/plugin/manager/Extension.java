package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.processor.ExtensionAnnotationProcessor;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


@Retention(RUNTIME)
@Target(TYPE)
@Inherited
@Documented
public @interface Extension {

    int ordinal() default 0;

    /**
     * Uma matriz de pontos de extensão, que são implementados por esta extensão.
     * Esta configuração explícita substitui a detecção automática de pontos de extensão no
     * {@link ExtensionAnnotationProcessor}.
     * <p>
     * No caso de sua extensão derivar diretamente de um ponto de extensão, este atributo NÃO é obrigatório.
     * Mas em certos cenários mais complexos </a> isso
     * pode ser útil para definir explicitamente os pontos de extensão para uma extensão.
     *
     * @return classes de pontos de extensão, que são implementadas por esta extensão
     */
    Class<? extends ExtensionPoint>[] points() default {};

    /**
     * Uma série de IDs de plug-ins, que devem estar disponíveis para carregar esta extensão.
     * O {@link AbstractExtensionFinder} não carregará esta extensão, se esses plug-ins não forem
     * disponível / iniciado em tempo de execução.
     * <p>
     * Aviso: este recurso requer a <a href="https://asm.ow2.io/"> biblioteca ASM </a> opcional
     * para estar disponível no classpath do aplicativo e deve ser explicitamente habilitado via
     * {@link AbstractExtensionFinder # setCheckForExtensionDependencies (boolean)}.
     *
     * @return archbasePlugin IDs, que devem estar disponíveis para carregar esta extensão
     */
    String[] plugins() default {};

}
