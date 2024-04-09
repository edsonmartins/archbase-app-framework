package br.com.archbase.plugin.manager.update;

import java.net.URL;
import java.util.Map;

/**
 * Contrato para repositórios de atualização.
 */
public interface UpdateRepository {

    /**
     * @return o ID deste repositório. Deve ser único
     */
    String getId();

    /**
     * @return o URL deste repositório como uma string
     */
    URL getUrl();

    /**
     * Obtenha todas as informações do plugin para este repositório.
     *
     * @return Mapa de PluginId e PluginInfo
     */
    Map<String, PluginInfo> getPlugins();

    /**
     * Obtenha informações de um plugin específico a partir deste repositório.
     *
     * @param id o id do plugin
     * @return the PluginInfo
     */
    PluginInfo getPlugin(String id);

    /**
     * Libera informações em cache para forçar a nova busca do estado do repositório na próxima obtenção.
     */
    void refresh();

    /**
     * Cada repositório tem a opção de substituir o processo de download.
     * Eles podem, por exemplo, fazer checksum, verificações de assinatura etc.
     * Para usar o downloader padrão, retorne null.
     *
     * @return the FileDownloader para usar para este repositório ou null se você não deseja substituir
     */
    FileDownloader getFileDownloader();

    /**
     * Obtém um verificador de arquivo a ser executado no arquivo baixado para que seja considerado válido.
     * Pode ser um CompoundVerifier para encadear vários verificadores.
     *
     * @return {@link FileVerifier}
     */
    FileVerifier getFileVerifier();

}
