package br.com.archbase.semver.implementation;

/**
 * A classe {@code NormalVersion} representa o núcleo da versão.
 * <p>
 * Esta classe é imutável e, portanto, thread-safe.
 */
class NormalVersion implements Comparable<NormalVersion> {

    /**
     * O número da versão principal.
     */
    private final int major;

    /**
     * O número da versão secundária.
     */
    private final int minor;

    /**
     * O número da versão do patch.
     */
    private final int patch;

    /**
     * Constrói uma {@code NormalVersion} com o
     * números de versão principais, secundários e de patch.
     *
     * @param major o número da versão principal
     * @param minor o número da versão secundária
     * @param patch o número da versão do patch
     * @throws IllegalArgumentException se um dos números da versão for um número inteiro negativo
     */
    NormalVersion(int major, int minor, int patch) {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException(
                    "As versões principais, secundárias e de patch DEVEM ser números inteiros não negativos."
            );
        }
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    /**
     * Retorna o número da versão principal.
     *
     * @return o número da versão principal
     */
    int getMajor() {
        return major;
    }

    /**
     * Retorna o número da versão secundária.
     *
     * @return o número da versão secundária
     */
    int getMinor() {
        return minor;
    }

    /**
     * Retorna o número da versão do patch.
     *
     * @return o número da versão do patch
     */
    int getPatch() {
        return patch;
    }

    /**
     * Incrementa o número da versão principal.
     *
     * @return uma nova instância da classe {@code NormalVersion}
     */
    NormalVersion incrementMajor() {
        return new NormalVersion(major + 1, 0, 0);
    }

    /**
     * Incrementa o número da versão secundária.
     *
     * @return uma nova instância da classe {@code NormalVersion}
     */
    NormalVersion incrementMinor() {
        return new NormalVersion(major, minor + 1, 0);
    }

    /**
     * Aumenta o número da versão do patch.
     *
     * @return uma nova instância da classe {@code NormalVersion}
     */
    NormalVersion incrementPatch() {
        return new NormalVersion(major, minor, patch + 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(NormalVersion other) {
        int result = major - other.major;
        if (result == 0) {
            result = minor - other.minor;
            if (result == 0) {
                result = patch - other.patch;
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof NormalVersion)) {
            return false;
        }
        return compareTo((NormalVersion) other) == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = 17;
        hash = 31 * hash + major;
        hash = 31 * hash + minor;
        hash = 31 * hash + patch;
        return hash;
    }

    /**
     * Retorna a representação de string desta versão normal.
     * <p>
     * Um número de versão normal DEVE ter o formato X.Y.Z, onde X, Y e Z são
     * inteiros não negativos. X é a versão principal, Y é a versão secundária,
     * e Z é a versão do patch. (SemVer p.2)
     *
     * @return a representação de string desta versão normal
     */
    @Override
    public String toString() {
        return String.format("%d.%d.%d", major, minor, patch);
    }
}
