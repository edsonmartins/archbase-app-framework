package br.com.archbase.plugin.manager;

import java.util.Arrays;
import java.util.List;

/**
 * {@link ClassLoadingStrategy} será usado para configurar a ordem de carregamento de {@link PluginClassLoader}
 * e contém todas as opções possíveis suportadas por {@link PluginClassLoader} onde:
 * {@code
 * A = fonte do aplicativo (carregar classes do classLoader pai)
 * P = Fonte do plug-in (carregar classes deste carregador de classe)
 * D = Dependências (carregar classes de dependências)
 * }
 */
public class ClassLoadingStrategy {

    /**
     * application(parent) -> archbasePlugin -> dependencies
     */
    public static final ClassLoadingStrategy APD = new ClassLoadingStrategy(Arrays.asList(Source.APPLICATION, Source.PLUGIN, Source.DEPENDENCIES));

    /**
     * application(parent) -> dependencies -> archbasePlugin
     */
    public static final ClassLoadingStrategy ADP = new ClassLoadingStrategy(Arrays.asList(Source.APPLICATION, Source.DEPENDENCIES, Source.PLUGIN));

    /**
     * archbasePlugin -> application(parent) -> dependencies
     */
    public static final ClassLoadingStrategy PAD = new ClassLoadingStrategy(Arrays.asList(Source.PLUGIN, Source.APPLICATION, Source.DEPENDENCIES));

    /**
     * dependencies -> application(parent) -> archbasePlugin
     */
    public static final ClassLoadingStrategy DAP = new ClassLoadingStrategy(Arrays.asList(Source.DEPENDENCIES, Source.APPLICATION, Source.PLUGIN));

    /**
     * dependencies -> archbasePlugin -> application(parent)
     */
    public static final ClassLoadingStrategy DPA = new ClassLoadingStrategy(Arrays.asList(Source.DEPENDENCIES, Source.PLUGIN, Source.APPLICATION));

    /**
     * archbasePlugin -> dependencies -> application(parent)
     */
    public static final ClassLoadingStrategy PDA = new ClassLoadingStrategy(Arrays.asList(Source.PLUGIN, Source.DEPENDENCIES, Source.APPLICATION));

    private final List<Source> sources;

    public ClassLoadingStrategy(List<Source> sources) {
        this.sources = sources;
    }

    public List<Source> getSources() {
        return sources;
    }

    public enum Source {
        PLUGIN, APPLICATION, DEPENDENCIES;
    }

}
