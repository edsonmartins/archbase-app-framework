package br.com.archbase.plugin.manager.update.verifier;

import br.com.archbase.plugin.manager.update.FileVerifier;
import br.com.archbase.plugin.manager.update.VerifyException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Verifica se o arquivo existe é um arquivo normal e tem um tamanho não nulo.
 */
public class BasicVerifier implements FileVerifier {

    /**
     * Verifica o lançamento de um plugin de acordo com certas regras.
     *
     * @param context o objeto de contexto do verificador de arquivo
     * @param file    o caminho para o próprio arquivo baixado
     * @throws IOException     se houver um problema ao acessar o arquivo
     * @throws VerifyException em caso de problemas ao verificar o arquivo
     */
    @Override
    public void verify(Context context, Path file) throws IOException {
        if (!Files.isRegularFile(file) || Files.size(file) == 0) {
            throw new VerifyException("O arquivo {} não é um arquivo normal ou tem tamanho 0", file);
        }
    }

}
