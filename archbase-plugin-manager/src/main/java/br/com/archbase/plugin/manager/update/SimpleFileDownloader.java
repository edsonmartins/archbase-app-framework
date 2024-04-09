package br.com.archbase.plugin.manager.update;

import br.com.archbase.plugin.manager.PluginRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;

/**
 * Downloads a file from a URL.
 */
public class SimpleFileDownloader implements FileDownloader {

    private static final Logger log = LoggerFactory.getLogger(SimpleFileDownloader.class);

    /**
     * Downloads a file. If HTTP(S) or FTP, stream content, if local file:/ do a simple filesystem copy to tmp folder.
     * Other protocols not supported.
     *
     * @param fileUrl the URI representing the file to download
     * @return the path of downloaded/copied file
     * @throws IOException            in case of network or IO problems
     * @throws PluginRuntimeException in case of other problems
     */
    public Path downloadFile(URL fileUrl) throws IOException {
        switch (fileUrl.getProtocol()) {
            case "http":
            case "https":
            case "ftp":
                return downloadFileHttp(fileUrl);
            case "file":
                return copyLocalFile(fileUrl);
            default:
                throw new PluginRuntimeException("URL protocol {} not supported", fileUrl.getProtocol());
        }
    }

    /**
     * Efficient copy of file in case of local file system.
     *
     * @param fileUrl source file
     * @return path of target file
     * @throws IOException            if problems during copy
     * @throws PluginRuntimeException in case of other problems
     */
    protected Path copyLocalFile(URL fileUrl) throws IOException {
        Path destination = Files.createTempDirectory("pf4j-update-downloader");
        destination.toFile().deleteOnExit();

        try {
            Path fromFile = Paths.get(fileUrl.toURI());
            String path = fileUrl.getPath();
            String fileName = path.substring(path.lastIndexOf('/') + 1);
            Path toFile = destination.resolve(fileName);
            Files.copy(fromFile, toFile, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);

            return toFile;
        } catch (URISyntaxException e) {
            throw new PluginRuntimeException("Something wrong with given URL", e);
        }
    }

    /**
     * Downloads file from HTTP or FTP.
     *
     * @param fileUrl source file
     * @return path of downloaded file
     * @throws IOException            if IO problems
     * @throws PluginRuntimeException if validation fails or any other problems
     */
    protected Path downloadFileHttp(URL fileUrl) throws IOException {
        Path destination = Files.createTempDirectory("pf4j-update-downloader");
        destination.toFile().deleteOnExit();

        String path = fileUrl.getPath();
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        Path file = destination.resolve(fileName);

        // set up the URL connection
        URLConnection connection = fileUrl.openConnection();

        // conectar ao site remoto (pode levar algum tempo)
        connection.connect();

        // verifique a autorização de http
        HttpURLConnection httpConnection = (HttpURLConnection) connection;
        if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            throw new ConnectException("HTTP Authorization failure");
        }

        // tente obter a data da última modificação especificada pelo servidor deste artefato
        long lastModified = httpConnection.getHeaderFieldDate("Last-Modified", System.currentTimeMillis());

        // tente obter o fluxo de entrada (três vezes)
        InputStream is = null;
        for (int i = 0; i < 3; i++) {
            try {
                is = connection.getInputStream();
                break;
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        if (is == null) {
            throw new ConnectException("Não foi possivel fazer download '" + fileUrl + " para '" + file + "'");
        }

        // ler do recurso remoto e escrever no arquivo local
        File toFile = file.toFile();
        try (FileOutputStream fos = new FileOutputStream(toFile)) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) >= 0) {
                fos.write(buffer, 0, length);
            }
        } finally {
            is.close();
        }

        log.debug("Definir última modificação de '{}' para '{}'", file, lastModified);
        Files.setLastModifiedTime(file, FileTime.fromMillis(lastModified));

        return file;
    }

}
