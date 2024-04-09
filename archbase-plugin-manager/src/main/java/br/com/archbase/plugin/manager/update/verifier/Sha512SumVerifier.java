package br.com.archbase.plugin.manager.update.verifier;

import br.com.archbase.plugin.manager.update.FileVerifier;
import br.com.archbase.plugin.manager.update.VerifyException;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Verifica se a soma de verificação SHA512 de um arquivo baixado é igual à soma de verificação fornecida em
 * o descritor plugins.json. Isso ajuda a validar se o arquivo baixado está exatamente
 * o mesmo que pretendido. Especialmente útil ao lidar com meta-repositórios apontando
 * para S3 ou outros locais de download de terceiros que possam ter sido adulterados.
 */
public class Sha512SumVerifier implements FileVerifier {

    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Verifica o lançamento de um plugin de acordo com certas regras
     *
     * @param context o objeto de contexto do verificador de arquivo
     * @param file    o caminho para o próprio arquivo baixado
     * @throws IOException     se houver um problema ao acessar o arquivo
     * @throws VerifyException em caso de problemas ao verificar o arquivo
     */
    @Override
    public void verify(Context context, Path file) throws IOException {
        String expectedSha512sum;
        try {
            if (context.getSha512sum() == null) {
                log.debug("Nenhuma soma de verificação sha512 especificada, ignorando a verificação");
                return;
            } else if (context.getSha512sum().equalsIgnoreCase(".sha512")) {
                String url = context.getUrl().substring(0, context.getUrl().lastIndexOf(".")) + ".sha512";
                expectedSha512sum = getUrlContents(url).split(" ")[0].trim();
            } else if (context.getSha512sum().startsWith("http")) {
                expectedSha512sum = getUrlContents(context.getSha512sum()).split(" ")[0].trim();
            } else {
                expectedSha512sum = context.getSha512sum();
            }
        } catch (IOException e) {
            throw new VerifyException(e, "SHA512 a verificação da soma de verificação falhou, não foi possível baixar o arquivo SHA512 ({})", context.getSha512sum());
        }

        log.debug("Verificando a soma de verificação sha512 do arquivo {}", file.getFileName());
        String actualSha512sum = DigestUtils.sha512Hex(Files.newInputStream(file));
        if (actualSha512sum.equalsIgnoreCase(expectedSha512sum)) {
            log.debug("Checksum OK");
            return;
        }
        throw new VerifyException("SHA512 soma de verificação do arquivo baixado " + file.getFileName()
                + " não corresponde ao do descritor do plugin. Obteve " + actualSha512sum
                + " mas esperado " + expectedSha512sum);
    }

    private String getUrlContents(String url) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new URL(url).openStream()))) {
            return reader.readLine();
        }
    }

}
