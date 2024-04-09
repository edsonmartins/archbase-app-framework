package br.com.archbase.plugin.manager.update;

import br.com.archbase.plugin.manager.PluginRuntimeException;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

/**
 * Interface para baixar um arquivo.
 */
public interface FileDownloader {

    /**
     * Baixa um arquivo para o destino. A implementação deve ser baixada para uma pasta temporária.
     * As implementações podem optar por oferecer suporte a diferentes protocolos, como http, https, ftp, arquivo ...
     * O caminho retornado deve ser de natureza temporária e provavelmente será movido / excluído pelo consumidor.
     *
     * @param fileUrl o URL que representa o arquivo para download
     * @return Caminho do arquivo baixado, normalmente em uma pasta temporária
     * @throws IOException            se houver um problema de IO durante o download
     * @throws PluginRuntimeException no caso de outros problemas, como protocolo não suportado
     */
    Path downloadFile(URL fileUrl) throws IOException;

}
