package br.com.archbase.plugin.manager;

import java.util.Comparator;

/**
 * Gerenciador responsável pelas versões dos plugins.
 */
public interface VersionManager {

    /**
     * Verifique se uma {@code constraint} e uma {@code version} correspondem.
     * Uma possível restrição pode ser {@code> = 1.0.0 & <2.0.0}.
     *
     * @param version
     * @param constraint
     * @return
     */
    boolean checkVersionConstraint(String version, String constraint);

    /**
     * Compare duas versões. É semelhante com {@link Comparator #compare(Object,Object)}.
     *
     * @param v1
     * @param v2
     */
    int compareVersions(String v1, String v2);

}
