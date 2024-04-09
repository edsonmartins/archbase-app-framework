package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.util.DirectedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

/**
 * Esta classe constrói um gráfico de dependência para uma lista de plug-ins (descritores).
 * O ponto de entrada é o método {@link #resolve (List)}, método que retorna um objeto {@link Result}.
 * A classe {@code Result} contém boas informações sobre o resultado da operação de resolução (se for uma dependência cíclica,
 * não são dependências encontradas, são dependências com versão errada).
 * Esta classe é muito útil para cenários if-else.
 * <p>
 * Apenas alguns atributos (pluginId, dependencies e pluginVersion) de {@link PluginDescriptor} são usados em
 * o processo de operação {@code resolve}.
 */

public class DependencyResolver {

    private static final Logger log = LoggerFactory.getLogger(DependencyResolver.class);

    private VersionManager versionManager;

    private DirectedGraph<String> dependenciesGraph; // o valor é 'pluginId'
    private DirectedGraph<String> dependentsGraph; // o valor é 'pluginId'
    private boolean resolved;

    public DependencyResolver(VersionManager versionManager) {
        this.versionManager = versionManager;
    }

    public Result resolve(List<PluginDescriptor> plugins) {
        // criar gráficos
        dependenciesGraph = new DirectedGraph<>();
        dependentsGraph = new DirectedGraph<>();

        // preencher gráficos
        Map<String, PluginDescriptor> pluginByIds = new HashMap<>();
        for (PluginDescriptor plugin : plugins) {
            addPlugin(plugin);
            pluginByIds.put(plugin.getPluginId(), plugin);
        }

        log.debug("Gráfico: {}", dependenciesGraph);

        // obter uma lista ordenada de dependências
        List<String> sortedPlugins = dependenciesGraph.reverseTopologicalSort();
        log.debug("Ordem de plugins: {}", sortedPlugins);

        // crie o objeto de resultado
        Result result = new Result(sortedPlugins);

        resolved = true;

        if (sortedPlugins != null) { // sem dependência cíclica
            // detectar dependências não encontradas
            for (String pluginId : sortedPlugins) {
                if (!pluginByIds.containsKey(pluginId)) {
                    result.addNotFoundDependency(pluginId);
                }
            }
        }

        // verificar versões de dependências
        for (PluginDescriptor plugin : plugins) {
            String pluginId = plugin.getPluginId();
            String existingVersion = plugin.getVersion();

            List<String> dependents = new ArrayList<>(getDependents(pluginId));
            while (!dependents.isEmpty()) {
                String dependentId = dependents.remove(0);
                PluginDescriptor dependent = pluginByIds.get(dependentId);
                String requiredVersion = getDependencyVersionSupport(dependent, pluginId);
                boolean ok = checkDependencyVersion(requiredVersion, existingVersion);
                if (!ok) {
                    result.addWrongDependencyVersion(new WrongDependencyVersion(pluginId, dependentId, existingVersion, requiredVersion));
                }
            }
        }

        return result;
    }

    /**
     * Recupera os ids de plug-ins dos quais o id de plug-in fornecido depende diretamente.
     *
     * @param pluginId o identificador de archbasePlugin exclusivo, especificado em seus metadados
     * @return
     */
    public List<String> getDependencies(String pluginId) {
        checkResolved();
        return dependenciesGraph.getNeighbors(pluginId);
    }

    /**
     * Recupera os ids de plug-ins dos quais o conteúdo fornecido é uma dependência direta.
     *
     * @param pluginId o identificador de archbasePlugin exclusivo, especificado em seus metadados
     * @return
     */
    public List<String> getDependents(String pluginId) {
        checkResolved();
        return dependentsGraph.getNeighbors(pluginId);
    }

    /**
     * Verifique se uma versão existente da dependência é compatível com a versão necessária (do descritor do archbasePlugin).
     *
     * @param requiredVersion
     * @param existingVersion
     * @return
     */
    protected boolean checkDependencyVersion(String requiredVersion, String existingVersion) {
        return versionManager.checkVersionConstraint(existingVersion, requiredVersion);
    }

    private void addPlugin(PluginDescriptor descriptor) {
        String pluginId = descriptor.getPluginId();
        List<PluginDependency> dependencies = descriptor.getDependencies();
        if (dependencies.isEmpty()) {
            dependenciesGraph.addVertex(pluginId);
            dependentsGraph.addVertex(pluginId);
        } else {
            boolean edgeAdded = false;
            for (PluginDependency dependency : dependencies) {
                // Não registre plug-ins opcionais no gráfico de dependência para evitar a desativação automática do plug-in,
                // if an optional dependency is missing.
                if (!dependency.isOptional()) {
                    edgeAdded = true;
                    dependenciesGraph.addEdge(pluginId, dependency.getPluginId());
                    dependentsGraph.addEdge(dependency.getPluginId(), pluginId);
                }
            }

            // Registre o archbasePlugin sem dependências, se todas as suas dependências forem opcionais.
            if (!edgeAdded) {
                dependenciesGraph.addVertex(pluginId);
                dependentsGraph.addVertex(pluginId);
            }
        }
    }

    private void checkResolved() {
        if (!resolved) {
            throw new IllegalStateException("Chame o método 'resolver' primeiro");
        }
    }

    private String getDependencyVersionSupport(PluginDescriptor dependent, String dependencyId) {
        List<PluginDependency> dependencies = dependent.getDependencies();
        for (PluginDependency dependency : dependencies) {
            if (dependencyId.equals(dependency.getPluginId())) {
                return dependency.getPluginVersionSupport();
            }
        }

        throw new IllegalStateException("Não é possível encontrar uma dependência com id '" + dependencyId +
                "' for archbasePlugin '" + dependent.getPluginId() + "'");
    }

    public static class Result {

        private boolean cyclicDependency;
        private List<String> notFoundDependencies; // o valor é "pluginId"
        private List<String> sortedPlugins; // o valor é "pluginId"
        private List<WrongDependencyVersion> wrongVersionDependencies;

        Result(List<String> sortedPlugins) {
            if (sortedPlugins == null) {
                cyclicDependency = true;
                this.sortedPlugins = Collections.emptyList();
            } else {
                this.sortedPlugins = new ArrayList<>(sortedPlugins);
            }

            notFoundDependencies = new ArrayList<>();
            wrongVersionDependencies = new ArrayList<>();
        }

        /**
         * Retorna verdadeiro se uma dependência cíclica foi detectada.
         */
        public boolean hasCyclicDependency() {
            return cyclicDependency;
        }

        /**
         * Retorna uma lista com as dependências necessárias que não foram encontradas.
         */
        public List<String> getNotFoundDependencies() {
            return notFoundDependencies;
        }

        /**
         * Retorna uma lista com dependências com versão incorreta.
         */
        public List<WrongDependencyVersion> getWrongVersionDependencies() {
            return wrongVersionDependencies;
        }

        /**
         * Obtenha a lista de plug-ins em ordem de classificação de dependência.
         */
        public List<String> getSortedPlugins() {
            return sortedPlugins;
        }

        void addNotFoundDependency(String pluginId) {
            notFoundDependencies.add(pluginId);
        }

        void addWrongDependencyVersion(WrongDependencyVersion wrongDependencyVersion) {
            wrongVersionDependencies.add(wrongDependencyVersion);
        }

    }

    public static class WrongDependencyVersion implements Serializable {

        private String dependencyId; // o valor é "pluginId"
        private String dependentId; // o valor é "pluginId"
        private String existingVersion;
        private String requiredVersion;

        WrongDependencyVersion(String dependencyId, String dependentId, String existingVersion, String requiredVersion) {
            this.dependencyId = dependencyId;
            this.dependentId = dependentId;
            this.existingVersion = existingVersion;
            this.requiredVersion = requiredVersion;
        }

        public String getDependencyId() {
            return dependencyId;
        }

        public String getDependentId() {
            return dependentId;
        }

        public String getExistingVersion() {
            return existingVersion;
        }

        public String getRequiredVersion() {
            return requiredVersion;
        }

        @Override
        public String toString() {
            return "WrongDependencyVersion{" +
                    "dependencyId='" + dependencyId + '\'' +
                    ", dependentId='" + dependentId + '\'' +
                    ", existingVersion='" + existingVersion + '\'' +
                    ", requiredVersion='" + requiredVersion + '\'' +
                    '}';
        }
    }

    /**
     * Ele será lançado se uma dependência cíclica for detectada.
     */
    public static class CyclicDependencyException extends PluginRuntimeException {

        public CyclicDependencyException() {
            super("Cyclic dependencies");
        }

    }

    /**
     * Indica que as dependências necessárias não foram encontradas.
     */
    @SuppressWarnings(("all"))
    public static class DependenciesNotFoundException extends PluginRuntimeException {

        private List<String> dependencies;

        public DependenciesNotFoundException(List<String> dependencies) {
            super("Dependências '{}' não encontradas", dependencies);

            this.dependencies = dependencies;
        }

        public List<String> getDependencies() {
            return dependencies;
        }

    }

    /**
     * Indica que algumas dependências possuem versão incorreta.
     */
    @SuppressWarnings("all")
    public static class DependenciesWrongVersionException extends PluginRuntimeException {

        private List<WrongDependencyVersion> dependencies;

        public DependenciesWrongVersionException(List<WrongDependencyVersion> dependencies) {
            super("Dependências '{}' tem versão errada", dependencies);

            this.dependencies = dependencies;
        }

        public List<WrongDependencyVersion> getDependencies() {
            return dependencies;
        }

    }

}

