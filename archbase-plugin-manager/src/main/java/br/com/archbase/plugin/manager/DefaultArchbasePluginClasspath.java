package br.com.archbase.plugin.manager;

/**
 * Os valores padrão são {@link #CLASSES_DIR} e {@code #LIB_DIR}.
 */
public class DefaultArchbasePluginClasspath extends PluginClasspath {

    public static final String CLASSES_DIR = "classes";
    public static final String LIB_DIR = "lib";

    public DefaultArchbasePluginClasspath() {
        super();

        addClassesDirectories(CLASSES_DIR);
        addJarsDirectories(LIB_DIR);
    }

}
