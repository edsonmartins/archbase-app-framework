package br.com.archbase.plugin.manager.update;


import br.com.archbase.plugin.manager.*;
import br.com.archbase.plugin.manager.update.verifier.CompoundVerifier;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;


@SuppressWarnings("all")
public class UpdateManager {

    private static final Logger log = LoggerFactory.getLogger(UpdateManager.class);
    protected List<UpdateRepository> repositories;
    private ArchbasePluginManager archbasePluginManager;
    private VersionManager versionManager;
    private String systemVersion;
    private Path repositoriesJson;
    // cache da última versão do plugin por id de plugin (a chave)
    private Map<String, PluginInfo.PluginRelease> lastPluginRelease = new HashMap<>();

    public UpdateManager(ArchbasePluginManager archbasePluginManager) {
        this.archbasePluginManager = archbasePluginManager;

        versionManager = archbasePluginManager.getVersionManager();
        systemVersion = archbasePluginManager.getSystemVersion();
        repositoriesJson = Paths.get("repositories.json");
    }

    public UpdateManager(ArchbasePluginManager archbasePluginManager, Path repositoriesJson) {
        this(archbasePluginManager);

        this.repositoriesJson = repositoriesJson;
    }

    public UpdateManager(ArchbasePluginManager archbasePluginManager, List<UpdateRepository> repos) {
        this(archbasePluginManager);

        repositories = repos == null ? new ArrayList<>() : repos;
    }

    public List<PluginInfo> getAvailablePlugins() {
        List<PluginInfo> availablePlugins = new ArrayList<>();
        for (PluginInfo plugin : getPlugins()) {
            if (archbasePluginManager.getPlugin(plugin.id) == null) {
                availablePlugins.add(plugin);
            }
        }

        return availablePlugins;
    }

    public boolean hasAvailablePlugins() {
        for (PluginInfo plugin : getPlugins()) {
            if (archbasePluginManager.getPlugin(plugin.id) == null) {
                return true;
            }
        }

        return false;
    }

    /**
     * Retorne uma lista de plug-ins que são versões mais recentes de plug-ins já instalados.
     *
     * @return lista de plugins que possuem atualizações
     */
    public List<PluginInfo> getUpdates() {
        List<PluginInfo> updates = new ArrayList<>();
        for (PluginWrapper installed : archbasePluginManager.getPlugins()) {
            String pluginId = installed.getPluginId();
            if (hasPluginUpdate(pluginId)) {
                updates.add(getPluginsMap().get(pluginId));
            }
        }

        return updates;
    }

    /**
     * Verifica se o Update Repositories possui versões mais recentes de alguns dos plug-ins instalados.
     *
     * @return verdadeiro se houver atualizações
     */
    public boolean hasUpdates() {
        return !getUpdates().isEmpty();
    }

    /**
     * Obtenha a lista de plug-ins de todos os repositórios.
     *
     * @return Lista de informações do plugin
     */
    public List<PluginInfo> getPlugins() {
        List<PluginInfo> list = new ArrayList<>(getPluginsMap().values());
        Collections.sort(list);

        return list;
    }

    /**
     * Obtenha um mapa de todos os plug-ins de todos os repositórios onde a chave é o id do plug-in.
     *
     * @return Lista de informações do plugin
     */
    public Map<String, PluginInfo> getPluginsMap() {
        Map<String, PluginInfo> pluginsMap = new HashMap<>();
        for (UpdateRepository repository : getRepositories()) {
            pluginsMap.putAll(repository.getPlugins());
        }

        return pluginsMap;
    }

    public List<UpdateRepository> getRepositories() {
        if (repositories == null && repositoriesJson != null) {
            refresh();
        }

        return repositories;
    }

    /**
     * Substitua todos os repositórios.
     *
     * @param repositories lista de novos repositórios
     */
    public void setRepositories(List<UpdateRepository> repositories) {
        this.repositories = repositories;
        refresh();
    }

    /**
     * Adicione um {@link DefaultUpdateRepository}.
     *
     * @param id  de repo
     * @param url de repo
     */
    public void addRepository(String id, URL url) {
        for (UpdateRepository ur : repositories) {
            if (ur.getId().equals(id)) {
                throw new PluginRuntimeException("Repositório com id" + id + " já existe");
            }
        }
        repositories.add(new DefaultUpdateRepository(id, url));
    }

    /**
     * Adicione um repo que foi criado pelo cliente.
     *
     * @param newRepo o novo UpdateRepository para adicionar à lista
     */
    public void addRepository(UpdateRepository newRepo) {
        for (UpdateRepository ur : repositories) {
            if (ur.getId().equals(newRepo.getId())) {
                throw new RuntimeException("Repositório com id " + newRepo.getId() + " já existe");
            }
        }
        newRepo.refresh();
        repositories.add(newRepo);
    }

    /**
     * Remova um repositório por id.
     *
     * @param id do repositório para remover
     */
    public void removeRepository(String id) {
        for (UpdateRepository repo : getRepositories()) {
            if (id.equals(repo.getId())) {
                repositories.remove(repo);
                break;
            }
        }
        log.warn("Repositório com id " + id + " não encontrado, não fazendo nada");
    }

    /**
     * Atualiza todos os repositórios, então eles são forçados a atualizar a lista de plug-ins.
     */
    public synchronized void refresh() {
        if (repositoriesJson != null && Files.exists(repositoriesJson)) {
            initRepositoriesFromJson();
        }
        for (UpdateRepository updateRepository : repositories) {
            updateRepository.refresh();
        }
        lastPluginRelease.clear();
    }

    /**
     * Instala um plugin por id e versão.
     *
     * @param id      o id do plugin para instalar
     * @param version a versão do plugin a ser instalado, no formato SemVer, ou null para o mais recente
     * @return true se a instalação for bem sucedida e o plugin iniciado
     * @throws PluginRuntimeException se o plugin não existe em repositórios ou problemas durante
     */
    public synchronized boolean installPlugin(String id, String version) {
        // Baixar para local temporário
        Path downloaded = downloadPlugin(id, version);

        Path pluginsRoot = archbasePluginManager.getPluginsRoot();
        Path file = pluginsRoot.resolve(downloaded.getFileName());
        try {
            Files.move(downloaded, file, REPLACE_EXISTING);
        } catch (IOException e) {
            throw new PluginRuntimeException(e, "Falha ao gravar o arquivo '{}' na pasta de plug-ins", file);
        }

        String pluginId = archbasePluginManager.loadPlugin(file);
        PluginState state = archbasePluginManager.startPlugin(pluginId);

        return PluginState.STARTED.equals(state);
    }

    /**
     * Faz o download de um plugin com as coordenadas fornecidas, executa todos {@link FileVerifier}s
     * e retorna um caminho para o arquivo baixado.
     *
     * @param id      of plugin
     * @param version do plugin ou nulo para baixar o mais recente
     * @return Caminho para o arquivo que residirá em uma pasta temporária na área temporária padrão do sistema
     * @throws PluginRuntimeException se o download falhou
     */
    protected Path downloadPlugin(String id, String version) {
        try {
            PluginInfo.PluginRelease release = findReleaseForPlugin(id, version);
            Path downloaded = getFileDownloader(id).downloadFile(new URL(release.url));
            getFileVerifier(id).verify(new FileVerifier.Context(id, release), downloaded);
            return downloaded;
        } catch (IOException e) {
            throw new PluginRuntimeException(e, "Erro durante o download do plugin " + id);
        }
    }

    /**
     * Encontra o {@link FileDownloader} para usar neste repositório.
     *
     * @param pluginId o plugin que desejamos baixar
     * @return Instância FileDownloader
     */
    protected FileDownloader getFileDownloader(String pluginId) {
        for (UpdateRepository ur : repositories) {
            if (ur.getPlugin(pluginId) != null && ur.getFileDownloader() != null) {
                return ur.getFileDownloader();
            }
        }

        return new SimpleFileDownloader();
    }

    /**
     * Obtém um verificador de arquivo para usar com este plugin. Primeiro tenta usar o verificador personalizado
     * configurado para o repositório, em seguida, voltar ao padrão {@link CompoundVerifier}
     *
     * @param pluginId o plugin que desejamos baixar
     * @return Instância FileVerifier
     */
    protected FileVerifier getFileVerifier(String pluginId) {
        for (UpdateRepository ur : repositories) {
            if (ur.getPlugin(pluginId) != null && ur.getFileVerifier() != null) {
                return ur.getFileVerifier();
            }
        }

        return new CompoundVerifier();
    }

    /**
     * Resolve liberação de id e versão.
     *
     * @param id      do plugin
     * @param version de plugin ou nulo para localizar a versão mais recente
     * @return PluginRelease para download
     * @throws PluginRuntimeException se id ou versão não existe
     */
    protected PluginInfo.PluginRelease findReleaseForPlugin(String id, String version) {
        PluginInfo pluginInfo = getPluginsMap().get(id);
        if (pluginInfo == null) {
            log.info("ArchbasePlugin com id {} não existe em nenhum repositório", id);
            throw new PluginRuntimeException("ArchbasePlugin com id {} não encontrado em nenhum repositório", id);
        }

        if (version == null) {
            return getLastPluginRelease(id);
        }

        for (PluginInfo.PluginRelease release : pluginInfo.releases) {
            if (versionManager.compareVersions(version, release.version) == 0 && release.url != null) {
                return release;
            }
        }

        throw new PluginRuntimeException("ArchbasePlugin {} com versão @ {} não existe no repositório", id, version);
    }

    /**
     * Atualiza um id de plugin para a versão fornecida ou para a versão mais recente se {@code version == null}.
     *
     * @param id      o id do plugin a ser atualizado
     * @param version a versão para a qual atualizar, no formato SemVer ou null para o mais recente
     * @return true se a atualização for bem-sucedida
     * @throws PluginRuntimeException caso a versão fornecida não esteja disponível, id do plugin ainda não instalado etc.
     */
    public boolean updatePlugin(String id, String version) {
        if (archbasePluginManager.getPlugin(id) == null) {
            throw new PluginRuntimeException("O plug-in {} não pode ser atualizado porque não está instalado", id);
        }

        PluginInfo pluginInfo = getPluginsMap().get(id);
        if (pluginInfo == null) {
            throw new PluginRuntimeException("ArchbasePlugin {} não existe em nenhum repositório", id);
        }

        if (!hasPluginUpdate(id)) {
            log.warn("O plug-in {} não tem uma atualização disponível que seja compatível com a versão do sistema {}", id, systemVersion);
            return false;
        }

        // Baixar para pasta temporária
        Path downloaded = downloadPlugin(id, version);

        if (!archbasePluginManager.deletePlugin(id)) {
            return false;
        }

        Path pluginsRoot = archbasePluginManager.getPluginsRoot();
        Path file = pluginsRoot.resolve(downloaded.getFileName());
        try {
            Files.move(downloaded, file, REPLACE_EXISTING);
        } catch (IOException e) {
            throw new PluginRuntimeException("Falha ao gravar arquivo de plugin {} na pasta de plugin ", file);
        }

        String newPluginId = archbasePluginManager.loadPlugin(file);
        PluginState state = archbasePluginManager.startPlugin(newPluginId);

        return PluginState.STARTED.equals(state);
    }

    public boolean uninstallPlugin(String id) {
        return archbasePluginManager.deletePlugin(id);
    }

    /**
     * Retorna a última versão de lançamento deste plugin para determinada versão do sistema, independentemente da data de lançamento.
     *
     * @return PluginRelease que tem o maior número de versão
     */
    public PluginInfo.PluginRelease getLastPluginRelease(String id) {
        PluginInfo pluginInfo = getPluginsMap().get(id);
        if (pluginInfo == null) {
            return null;
        }

        if (!lastPluginRelease.containsKey(id)) {
            for (PluginInfo.PluginRelease release : pluginInfo.releases) {
                if (systemVersion.equals("0.0.0") || versionManager.checkVersionConstraint(systemVersion, release.requires)) {
                    if (lastPluginRelease.get(id) == null) {
                        lastPluginRelease.put(id, release);
                    } else if (versionManager.compareVersions(release.version, lastPluginRelease.get(id).version) > 0) {
                        lastPluginRelease.put(id, release);
                    }
                }
            }
        }

        return lastPluginRelease.get(id);
    }

    /**
     * Verifica se a versão mais recente do plugin.
     *
     * @return true se houver uma versão mais recente disponível que seja compatível com o sistema
     */
    public boolean hasPluginUpdate(String id) {
        PluginInfo pluginInfo = getPluginsMap().get(id);
        if (pluginInfo == null) {
            return false;
        }

        String installedVersion = archbasePluginManager.getPlugin(id).getDescriptor().getVersion();
        PluginInfo.PluginRelease last = getLastPluginRelease(id);

        return last != null && versionManager.compareVersions(last.version, installedVersion) > 0;
    }

    protected synchronized void initRepositoriesFromJson() {
        log.debug("Read repositories from '{}'", repositoriesJson);
        try (FileReader reader = new FileReader(repositoriesJson.toFile())) {
            Gson gson = new GsonBuilder().create();
            UpdateRepository[] items = gson.fromJson(reader, DefaultUpdateRepository[].class);
            repositories = Arrays.asList(items);
        } catch (IOException e) {
            e.printStackTrace();
            repositories = Collections.emptyList();
        }
    }

}
