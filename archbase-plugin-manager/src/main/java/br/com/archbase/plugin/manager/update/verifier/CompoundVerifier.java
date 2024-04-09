package br.com.archbase.plugin.manager.update.verifier;

import br.com.archbase.plugin.manager.update.FileVerifier;
import br.com.archbase.plugin.manager.update.VerifyException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CompoundVerifier implements FileVerifier {

    /**
     * Lista padrão de verificadores
     */
    protected static final List<FileVerifier> ALL_DEFAULT_FILE_VERIFIERS = Arrays.asList(
            new BasicVerifier(),
            new Sha512SumVerifier());

    private List<FileVerifier> verifiers = new ArrayList<>();

    /**
     * Construtor padrão que irá adicionar os verificadores padrão para começar
     */
    public CompoundVerifier() {
        setVerifiers(ALL_DEFAULT_FILE_VERIFIERS);
    }

    /**
     * Constrói um verificador composto usando a lista fornecida de verificadores em vez dos padrões
     *
     * @param verifiers a lista de verificadores a serem aplicados
     */
    public CompoundVerifier(List<FileVerifier> verifiers) {
        this.verifiers = verifiers;
    }

    /**
     * Verifica a versão do plug-in usando todos os {@link FileVerifier} s configurados
     *
     * @param context o objeto de contexto do verificador de arquivo
     * @param file    o caminho para o próprio arquivo baixado
     * @throws IOException     se houver um problema ao acessar o arquivo
     * @throws VerifyException em caso de problemas ao verificar o arquivo
     */
    @Override
    public void verify(Context context, Path file) throws IOException {
        for (FileVerifier verifier : getVerifiers()) {
            verifier.verify(context, file);
        }
    }

    public List<FileVerifier> getVerifiers() {
        return verifiers;
    }

    public void setVerifiers(List<FileVerifier> verifiers) {
        this.verifiers = verifiers;
    }

}
