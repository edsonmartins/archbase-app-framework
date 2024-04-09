package br.com.archbase.plugin.manager;

/**
 * É um {@link PluginClasspath} composto ({@link #MAVEN} + {@link #GRADLE} + {@link #KOTLIN})
 * usado no modo de desenvolvimento ({@link RuntimeMode#DEVELOPMENT}).
 */
public class DevelopmentPluginClasspath extends PluginClasspath {

    /**
     * O classpath do archbasePlugin de desenvolvimento para <a href="https://maven.apache.org"> Maven </a>.
     * O diretório de classes é {@code target/classes} e o diretório lib é {@code target/lib}.
     */
    public static final PluginClasspath MAVEN = new PluginClasspath().addClassesDirectories("target/classes").addJarsDirectories("target/lib");

    /**
     * O classpath do archbasePlugin de desenvolvimento para <a href="https://gradle.org"> Gradle </a>.
     * Os diretórios de classes são {@code build/classes/java/main, build/resources/main}.
     */
    public static final PluginClasspath GRADLE = new PluginClasspath().addClassesDirectories("build/classes/java/main", "build/resources/main");

    /**
     * O classpath do archbasePlugin de desenvolvimento para <a href="https://kotlinlang.org"> Kotlin </a>.
     * Os diretórios de classes são {@code build/classes/kotlin/main ", build/resources/main, build/tmp/kapt3/classes/main}.
     */
    public static final PluginClasspath KOTLIN = new PluginClasspath().addClassesDirectories("build/classes/kotlin/main", "build/resources/main", "build/tmp/kapt3/classes/main");

    /**
     * O classpath do archbasePlugin de desenvolvimento para <a href="https://www.jetbrains.com/help/idea/specifying-compilation-settings.html"> IDEA </a>.
     * Os diretórios de classes são {@code out/production/classes ", out/production/resource}.
     */
    public static final PluginClasspath IDEA = new PluginClasspath().addClassesDirectories("out/production/classes", "out/production/resource");

    public DevelopmentPluginClasspath() {
        addClassesDirectories(MAVEN.getClassesDirectories());
        addClassesDirectories(GRADLE.getClassesDirectories());
        addClassesDirectories(KOTLIN.getClassesDirectories());
        addClassesDirectories(IDEA.getClassesDirectories());

        addJarsDirectories(MAVEN.getJarsDirectories());
        addJarsDirectories(GRADLE.getJarsDirectories());
        addJarsDirectories(KOTLIN.getJarsDirectories());
        addJarsDirectories(IDEA.getJarsDirectories());
    }

}
