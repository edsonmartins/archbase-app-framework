package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.util.StringUtils;
import br.com.archbase.semver.implementation.Version;
import br.com.archbase.semver.implementation.expr.Expression;

/**
 * Implementação padrão para {@link VersionManager}.
 * Esta implementação usa jSemVer (uma implementação Java da Especificação SemVer).
 */
public class DefaultArchbaseVersionManager implements VersionManager {

    /**
     * Verifica se uma versão satisfaz a string SemVer {@link Expression} especificada.
     * Se a restrição for vazia ou nula, o método retornará verdadeiro.
     * Exemplos de restrição: {@code> 2.0.0} (simples), {@code "> = 1.4.0 & <1.6.0"} (intervalo).
     *
     * @param version
     * @param constraint
     * @return
     */
    @Override
    public boolean checkVersionConstraint(String version, String constraint) {
        return StringUtils.isNullOrEmpty(constraint) || Version.valueOf(version).satisfies(constraint);
    }

    @Override
    public int compareVersions(String v1, String v2) {
        return Version.valueOf(v1).compareTo(Version.valueOf(v2));
    }

}
