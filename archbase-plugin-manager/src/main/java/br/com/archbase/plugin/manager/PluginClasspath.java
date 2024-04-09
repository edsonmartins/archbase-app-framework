package br.com.archbase.plugin.manager;

import java.util.*;

/**
 * O classpath do archbasePlugin.
 * Contém diretórios {@code classes} (diretórios que contêm arquivos de classes)
 * e diretórios {@code jars} (diretórios que contêm arquivos jars).
 */
public class PluginClasspath {

    private Set<String> classesDirectories = new HashSet<>();
    private Set<String> jarsDirectories = new HashSet<>();

    public Set<String> getClassesDirectories() {
        return classesDirectories;
    }

    public PluginClasspath addClassesDirectories(String... classesDirectories) {
        return addClassesDirectories(Arrays.asList(classesDirectories));
    }

    public PluginClasspath addClassesDirectories(Collection<String> classesDirectories) {
        this.classesDirectories.addAll(classesDirectories);

        return this;
    }

    public Set<String> getJarsDirectories() {
        return jarsDirectories;
    }

    public PluginClasspath addJarsDirectories(String... jarsDirectories) {
        return addJarsDirectories(Arrays.asList(jarsDirectories));
    }

    public PluginClasspath addJarsDirectories(Collection<String> jarsDirectories) {
        this.jarsDirectories.addAll(jarsDirectories);

        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PluginClasspath)) return false;
        PluginClasspath that = (PluginClasspath) o;
        return classesDirectories.equals(that.classesDirectories) &&
                jarsDirectories.equals(that.jarsDirectories);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classesDirectories, jarsDirectories);
    }
}
