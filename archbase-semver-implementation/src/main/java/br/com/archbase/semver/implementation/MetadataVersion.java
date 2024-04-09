package br.com.archbase.semver.implementation;

import java.util.Arrays;

/**
 * A classe {@code MetadataVersion} é usada para representar
 * a versão de pré-lançamento e os metadados do build.
 */
class MetadataVersion implements Comparable<MetadataVersion> {

    /**
     * Metadados nulos, a implementação do padrão de design Objeto Nulo.
     */
    @SuppressWarnings("java:S2390")
    static MetadataVersion metadataNull = new NullMetadataVersion();
    /**
     * O array contendo os identificadores da versão.
     */
    private final String[] idents;

    /**
     * Constrói uma instância {@code MetadataVersion} com identificadores.
     *
     * @param identifiers os identificadores da versão
     */
    MetadataVersion(String[] identifiers) {
        idents = identifiers;
    }

    /**
     * Incrementa a versão dos metadados.
     *
     * @return uma nova instância da classe {@code MetadataVersion}
     */
    MetadataVersion increment() {
        String[] ids = idents;
        String lastId = ids[ids.length - 1];
        if (isInt(lastId)) {
            int intId = Integer.parseInt(lastId);
            ids[ids.length - 1] = String.valueOf(++intId);
        } else {
            ids = Arrays.copyOf(ids, ids.length + 1);
            ids[ids.length - 1] = String.valueOf(1);
        }
        return new MetadataVersion(ids);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MetadataVersion)) {
            return false;
        }
        return compareTo((MetadataVersion) other) == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(idents);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String ident : idents) {
            sb.append(ident).append(".");
        }
        return sb.deleteCharAt(sb.lastIndexOf(".")).toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(MetadataVersion other) {
        if (other == MetadataVersion.metadataNull) {
            /**
             * As versões de pré-lançamento têm uma precedência menor do que
             * a versão normal associada. (SemVer p.9)
             */
            return -1;
        }
        int result = compareIdentifierArrays(other.idents);
        if (result == 0) {
            /**
             * Um conjunto maior de campos de pré-lançamento tem um maior
             * precedência do que um conjunto menor, se todos os
             * os identificadores anteriores são iguais. (SemVer p.11)
             */
            result = idents.length - other.idents.length;
        }
        return result;
    }

    /**
     * Compara duas matrizes de identificadores.
     *
     * @param otherIdents os identificadores da outra versão
     * @return resultado inteiro de comparação compatível com
     * o método {@code Comparable.compareTo}
     */
    private int compareIdentifierArrays(String[] otherIdents) {
        int result = 0;
        int length = getLeastCommonArrayLength(idents, otherIdents);
        for (int i = 0; i < length; i++) {
            result = compareIdentifiers(idents[i], otherIdents[i]);
            if (result != 0) {
                break;
            }
        }
        return result;
    }

    /**
     * Retorna o tamanho da menor matriz.
     *
     * @param arr1 o primeiro array
     * @param arr2 o segundo array
     * @return o tamanho do menor array
     */
    private int getLeastCommonArrayLength(String[] arr1, String[] arr2) {
        return arr1.length <= arr2.length ? arr1.length : arr2.length;
    }

    /**
     * Compara dois identificadores.
     *
     * @param ident1 o primeiro identificador
     * @param ident2 o segundo identificador
     * @return resultado inteiro de comparação compatível com
     * o método {@code Comparable.compareTo}
     */
    private int compareIdentifiers(String ident1, String ident2) {
        if (isInt(ident1) && isInt(ident2)) {
            return Integer.parseInt(ident1) - Integer.parseInt(ident2);
        } else {
            return ident1.compareTo(ident2);
        }
    }

    /**
     * Verifica se a string especificada é um inteiro.
     *
     * @param str a string para verificar
     * @return {@code true} se a string especificada for um inteiro
     * ou {@code false} caso contrário
     */
    private boolean isInt(String str) {
        try {
            Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    /**
     * Metadados nulos, uma implementação do padrão de design Objeto Nulo.
     */
    private static class NullMetadataVersion extends MetadataVersion {

        /**
         * Constrói uma instância {@code NullMetadataVersion}.
         */
        public NullMetadataVersion() {
            super(null);
        }

        /**
         * @throws NullPointerException como metadados nulos não podem ser incrementados
         */
        @Override
        MetadataVersion increment() {
            throw new NullPointerException("Metadata version is NULL");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object other) {
            return other instanceof NullMetadataVersion;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(MetadataVersion other) {
            if (!equals(other)) {
                /**
                 * As versões de pré-lançamento têm uma precedência menor do que
                 * a versão normal associada. (SemVer p.9)
                 */
                return 1;
            }
            return 0;
        }
    }
}
