package br.com.archbase.plugin.manager.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Esta classe extrai o conteúdo do zip do plugin em um diretório.
 * É uma aula apenas para uso interno.
 */
public class Unzip {

    private static final Logger log = LoggerFactory.getLogger(Unzip.class);

    /**
     * Contém o diretório de destino.
     * O arquivo será descompactado no diretório de destino.
     */
    private File destination;

    /**
     * Contém o caminho para o arquivo zip.
     */
    private File source;

    public Unzip() {
    }

    public Unzip(File source, File destination) {
        this.source = source;
        this.destination = destination;
    }

    private static void mkdirsOrThrow(File dir) throws IOException {
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Falha ao criar diretório " + dir);
        }
    }

    public void setSource(File source) {
        this.source = source;
    }

    public void setDestination(File destination) {
        this.destination = destination;
    }

    /**
     * Extraia o conteúdo do arquivo zip ({@code source}) para o diretório de destino.
     * Se o diretório de destino já existir, ele será excluído antes.
     */
    public void extract() throws IOException {
        log.debug("Extraindo o conteúdo de '{}' para '{}'", source, destination);

        // exclua o diretório de destino se existir
        if (destination.exists() && destination.isDirectory()) {
            FileUtils.delete(destination.toPath());
        }

        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(source))) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                File file = new File(destination, zipEntry.getName());

                // crie diretórios intermediários - às vezes, o zip não os adiciona
                File dir = new File(file.getParent());

                mkdirsOrThrow(dir);

                if (zipEntry.isDirectory()) {
                    mkdirsOrThrow(file);
                } else {
                    byte[] buffer = new byte[1024];
                    int length;
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        while ((length = zipInputStream.read(buffer)) >= 0) {
                            fos.write(buffer, 0, length);
                        }
                    }
                }
            }
        }
    }

}
