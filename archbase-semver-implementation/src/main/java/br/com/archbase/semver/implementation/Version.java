package br.com.archbase.semver.implementation;

import br.com.archbase.semver.implementation.expr.Expression;
import br.com.archbase.semver.implementation.expr.ExpressionParser;
import br.com.archbase.semver.implementation.expr.LexerException;
import br.com.archbase.semver.implementation.expr.UnexpectedTokenException;

import java.util.Comparator;

/**
 * A classe {@code Version} é a classe principal da biblioteca Java SemVer.
 * <p>
 * Esta classe implementa o padrão de projeto Facade.
 * Também é imutável, o que torna a classe thread-safe.
 */
public class Version implements Comparable<Version> {

    /**
     * Um comparador que respeita os metadados de construção ao comparar versões.
     */
    public static final Comparator<Version> BUILD_AWARE_ORDER = new BuildAwareOrder();
    /**
     * Um separador que separa o pré-lançamento
     * versão da versão normal.
     */
    private static final String PRE_RELEASE_PREFIX = "-";
    /**
     * Um separador que separa os metadados de construção de
     * a versão normal ou a versão de pré-lançamento.
     */
    private static final String BUILD_PREFIX = "+";
    /**
     * A versão normal.
     */
    private final NormalVersion normal;
    /**
     * A versão de pré-lançamento.
     */
    private final MetadataVersion preRelease;
    /**
     * Os metadados de construção.
     */
    private final MetadataVersion build;

    /**
     * Constrói uma instância de {@code Version} com a versão normal.
     *
     * @param normal a versão normal
     */
    Version(NormalVersion normal) {
        this(normal, MetadataVersion.metadataNull, MetadataVersion.metadataNull);
    }

    /**
     * Constrói uma instância de {@code Version} com o
     * versão normal e a versão de pré-lançamento.
     *
     * @param normal     a versão normal
     * @param preRelease a versão de pré-lançamento
     */
    Version(NormalVersion normal, MetadataVersion preRelease) {
        this(normal, preRelease, MetadataVersion.metadataNull);
    }

    /**
     * Constrói uma instância {@code Version} com o normal
     * versão, a versão de pré-lançamento e os metadados do build.
     *
     * @param normal     a versão normal
     * @param preRelease a versão de pré-lançamento
     * @param build      os metadados de construção
     */
    Version(
            NormalVersion normal,
            MetadataVersion preRelease,
            MetadataVersion build
    ) {
        this.normal = normal;
        this.preRelease = preRelease;
        this.build = build;
    }

    /**
     * Cria uma nova instância de {@code Version} como um
     * resultado da análise da string de versão especificada.
     *
     * @param version a string de versão a ser analisada
     * @return uma nova instância da classe {@code Version}
     * @throws IllegalArgumentException     se a string de entrada for {@code NULL} ou vazia
     * @throws ParseException               quando uma string de versão inválida é fornecida
     * @throws UnexpectedCharacterException é um caso especial de {@code ParseException}
     */
    public static Version valueOf(String version) {
        return VersionParser.parseValidSemVer(version);
    }

    /**
     * Cria uma nova instância de {@code Version}
     * para os números de versão especificados.
     *
     * @param major o número da versão principal
     * @return uma nova instância da classe {@code Version}
     * @throws IllegalArgumentException se um número inteiro negativo for passado
     */
    public static Version forIntegers(int major) {
        return new Version(new NormalVersion(major, 0, 0));
    }

    /**
     * Cria uma nova instância de {@code Version}
     * para os números de versão especificados.
     *
     * @param major o número da versão principal
     * @param minor o número da versão secundária
     * @return uma nova instância da classe {@code Version}
     * @throws IllegalArgumentException se um número inteiro negativo for passado
     */
    public static Version forIntegers(int major, int minor) {
        return new Version(new NormalVersion(major, minor, 0));
    }

    /**
     * Cria uma nova instância de {@code Version}
     * para os números de versão especificados.
     *
     * @param major o número da versão principal
     * @param minor o número da versão secundária
     * @param patch o número da versão do patch
     * @return uma nova instância da classe {@code Version}
     * @throws IllegalArgumentException se um número inteiro negativo for passado
     */
    public static Version forIntegers(int major, int minor, int patch) {
        return new Version(new NormalVersion(major, minor, patch));
    }

    /**
     * Verifica se esta versão satisfaz a string de Expressão SemVer especificada.
     * <p>
     * Este método faz parte da API SemVer Expressions.
     *
     * @param expr a string SemVer Expression
     * @return {@code true} se esta versão satisfizer o especificado
     * Expressão SemVer ou {@code false} caso contrário
     * @throws ParseException           no caso de um erro de análise geral
     * @throws LexerException           quando encontra um caracter ilegal
     * @throws UnexpectedTokenException quando se depara com um token inesperado
     */
    public boolean satisfies(String expr) {
        Parser<Expression> parser = ExpressionParser.newInstance();
        return satisfies(parser.parse(expr));
    }

    /**
     * Verifica se esta versão satisfaz a expressão SemVer especificada.
     * <p>
     * Este método faz parte da API SemVer Expressions.
     *
     * @param expr a expressão SemVer
     * @return {@code true} se esta versão satisfizer o especificado
     * Expressão SemVer ou {@code false} caso contrário
     */
    public boolean satisfies(Expression expr) {
        return expr.interpret(this);
    }

    /**
     * Incrementa a versão principal.
     *
     * @return uma nova instância da classe {@code Version}
     */
    public Version incrementMajorVersion() {
        return new Version(normal.incrementMajor());
    }

    /**
     * Incrementa a versão principal e acrescenta a versão de pré-lançamento.
     *
     * @param preRelease a versão de pré-lançamento para anexar
     * @return uma nova instância da classe {@code Version}
     * @throws IllegalArgumentException     se a string de entrada for {@code NULL} ou vazia
     * @throws ParseException               quando uma string de versão inválida é fornecida
     * @throws UnexpectedCharacterException é um caso especial de {@code ParseException}
     */
    public Version incrementMajorVersion(String preRelease) {
        return new Version(
                normal.incrementMajor(),
                VersionParser.parsePreRelease(preRelease)
        );
    }

    /**
     * Aumenta a versão secundária.
     *
     * @return uma nova instância da classe {@code Version}
     */
    public Version incrementMinorVersion() {
        return new Version(normal.incrementMinor());
    }

    /**
     * Incrementa a versão secundária e acrescenta a versão de pré-lançamento.
     *
     * @param preRelease a versão de pré-lançamento para anexar
     * @return uma nova instância da classe {@code Version}
     * @throws IllegalArgumentException     se a string de entrada for {@code NULL} ou vazia
     * @throws ParseException               quando uma string de versão inválida é fornecida
     * @throws UnexpectedCharacterException é um caso especial de {@code ParseException}
     */
    public Version incrementMinorVersion(String preRelease) {
        return new Version(
                normal.incrementMinor(),
                VersionParser.parsePreRelease(preRelease)
        );
    }

    /**
     * Incrementa a versão do patch.
     *
     * @return uma nova instância da classe {@code Version}
     */
    public Version incrementPatchVersion() {
        return new Version(normal.incrementPatch());
    }

    /**
     * Incrementa a versão do patch e acrescenta a versão de pré-lançamento.
     *
     * @param preRelease a versão de pré-lançamento para anexar
     * @return uma nova instância da classe {@code Version}
     * @throws IllegalArgumentException     se a string de entrada for {@code NULL} ou vazia
     * @throws ParseException               quando uma string de versão inválida é fornecida
     * @throws UnexpectedCharacterException é um caso especial de {@code ParseException}
     */
    public Version incrementPatchVersion(String preRelease) {
        return new Version(
                normal.incrementPatch(),
                VersionParser.parsePreRelease(preRelease)
        );
    }

    /**
     * Incrementa a versão de pré-lançamento.
     *
     * @return uma nova instância da classe {@code Version}
     */
    public Version incrementPreReleaseVersion() {
        return new Version(normal, preRelease.increment());
    }

    /**
     * Incrementa os metadados de construção.
     *
     * @return uma nova instância da classe {@code Version}
     */
    public Version incrementBuildMetadata() {
        return new Version(normal, preRelease, build.increment());
    }

    /**
     * Retorna o número da versão principal.
     *
     * @return o número da versão principal
     */
    public int getMajorVersion() {
        return normal.getMajor();
    }

    /**
     * Retorna o número da versão secundária.
     *
     * @return o número da versão secundária
     */
    public int getMinorVersion() {
        return normal.getMinor();
    }

    /**
     * Retorna o número da versão do patch.
     *
     * @return o número da versão do patch
     */
    public int getPatchVersion() {
        return normal.getPatch();
    }

    /**
     * Retorna a representação de string da versão normal.
     *
     * @return a representação de string da versão normal
     */
    public String getNormalVersion() {
        return normal.toString();
    }

    /**
     * Retorna a representação da string da versão de pré-lançamento.
     *
     * @return a representação da string da versão de pré-lançamento
     */
    public String getPreReleaseVersion() {
        return preRelease.toString();
    }

    /**
     * Define a versão de pré-lançamento.
     *
     * @param preRelease a versão de pré-lançamento para definir
     * @return uma nova instância da classe {@code Version}
     * @throws IllegalArgumentException     se a string de entrada for {@code NULL} ou vazia
     * @throws ParseException               quando uma string de versão inválida é fornecida
     * @throws UnexpectedCharacterException é um caso especial de {@code ParseException}
     */
    public Version setPreReleaseVersion(String preRelease) {
        return new Version(normal, VersionParser.parsePreRelease(preRelease));
    }

    /**
     * Retorna a representação de string dos metadados de construção.
     *
     * @return a representação de string dos metadados de construção
     */
    public String getBuildMetadata() {
        return build.toString();
    }

    /**
     * Define os metadados de construção.
     *
     * @param build os metadados de construção para definir
     * @return uma nova instância da classe {@code Version}
     * @throws IllegalArgumentException     se a string de entrada for {@code NULL} ou vazia
     * @throws ParseException               quando uma string de versão inválida é fornecida
     * @throws UnexpectedCharacterException é um caso especial de {@code ParseException}
     */
    public Version setBuildMetadata(String build) {
        return new Version(normal, preRelease, VersionParser.parseBuild(build));
    }

    /**
     * Verifica se esta versão é superior à outra.
     *
     * @param other versão para comparar
     * @return {@code true} se esta versão for superior à outra versão
     * ou {@code false} caso contrário
     * @see #compareTo (versão outra)
     */
    public boolean greaterThan(Version other) {
        return compareTo(other) > 0;
    }

    /**
     * Verifica se esta versão é maior ou igual à outra versão.
     *
     * @param other versão para comparar
     * @return {@code true} se esta versão for maior ou igual
     * para a outra versão ou {@code false} caso contrário
     * @see #compareTo(Version other)
     */
    public boolean greaterThanOrEqualTo(Version other) {
        return compareTo(other) >= 0;
    }

    /**
     * Verifica se esta versão é anterior à outra.
     *
     * @param other versão para comparar
     * @return {@code true} se esta versão for inferior à outra versão
     * ou {@code false} caso contrário
     * @see #compareTo(Version other)
     */
    public boolean lessThan(Version other) {
        return compareTo(other) < 0;
    }

    /**
     * Verifica se esta versão é menor ou igual à outra versão.
     *
     * @param other versão para comparar
     * @return {@code true} se esta versão for menor ou igual
     * para a outra versão ou {@code false} caso contrário
     * @see #compareTo(Version other)
     */
    public boolean lessThanOrEqualTo(Version other) {
        return compareTo(other) <= 0;
    }

    /**
     * Verifica se esta versão é igual à outra.
     * <p>
     * A comparação é feita pelo método {@code Version.compareTo}.
     *
     * @param other versão para comparar
     * @return {@code true} se esta versão for igual à outra versão
     * ou {@code false} caso contrário
     * @see #compareTo(Version other)
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Version)) {
            return false;
        }
        return compareTo((Version) other) == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + normal.hashCode();
        hash = 97 * hash + preRelease.hashCode();
        return hash;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getNormalVersion());
        if (preRelease != null && !getPreReleaseVersion().isEmpty()) {
            sb.append(PRE_RELEASE_PREFIX).append(getPreReleaseVersion());
        }
        if (build != null && !getBuildMetadata().isEmpty()) {
            sb.append(BUILD_PREFIX).append(getBuildMetadata());
        }
        return sb.toString();
    }

    /**
     * Compara esta versão com a outra versão.
     * <p>
     * Este método não leva em consideração a construção das versões
     * metadados. Se você quiser comparar os metadados de compilação das versões
     * use o método {@code Version.compareWithBuildsTo} ou o
     * Comparador {@code Version.BUILD_AWARE_ORDER}.
     *
     * @param other versão para comparar
     * @return um inteiro negativo, zero ou um inteiro positivo se esta versão
     * é menor, igual ou maior que a versão especificada
     * @see #BUILD_AWARE_ORDER
     * @see #compareWithBuildsTo(Version other)
     */
    @Override
    public int compareTo(Version other) {
        int result = normal.compareTo(other.normal);
        if (result == 0) {
            if (preRelease != null) {
                result = preRelease.compareTo(other.preRelease);
            }
        }
        return result;
    }

    /**
     * Compare esta versão com a outra versão
     * levando em consideração os metadados de construção.
     * <p>
     * O método usa o comparador {@code Version.BUILD_AWARE_ORDER}.
     *
     * @param other versão para comparar
     * @return resultado inteiro de comparação compatível com
     * do método {@code Comparable.compareTo}
     * @see #BUILD_AWARE_ORDER
     */
    public int compareWithBuildsTo(Version other) {
        return BUILD_AWARE_ORDER.compare(this, other);
    }

    /**
     * Um construtor mutável para a classe imutável {@code Version}.
     */
    public static class Builder {

        /**
         * A string de versão normal.
         */
        private String normal;

        /**
         * The pre-release version string.
         */
        private String preRelease;

        /**
         * A string de metadados de construção.
         */
        private String build;

        /**
         * Constrói uma instância {@code Builder}.
         */
        public Builder() {

        }

        /**
         * Constrói uma instância do {@code Builder} com o
         * representação em string da versão normal.
         *
         * @param normal a representação em string da versão normal
         */
        public Builder(String normal) {
            this.normal = normal;
        }

        /**
         * Define a versão normal.
         *
         * @param normal a representação em string da versão normal
         * @return this builder instance
         */
        public Builder setNormalVersion(String normal) {
            this.normal = normal;
            return this;
        }

        /**
         * Define a versão de pré-lançamento.
         *
         * @param preRelease a representação da string da versão de pré-lançamento
         * @return this builder instance
         */
        public Builder setPreReleaseVersion(String preRelease) {
            this.preRelease = preRelease;
            return this;
        }

        /**
         * Define os metadados de construção.
         *
         * @param build a representação de string dos metadados de construção
         * @return this builder instance
         */
        public Builder setBuildMetadata(String build) {
            this.build = build;
            return this;
        }

        /**
         * Constrói um objeto {@code Version}.
         *
         * @return uma instância recém-construída de {@code Version}
         * @throws ParseException               quando uma string de versão inválida é fornecida
         * @throws UnexpectedCharacterException é um caso especial de {@code ParseException}
         */
        public Version build() {
            StringBuilder sb = new StringBuilder();
            if (isFilled(normal)) {
                sb.append(normal);
            }
            if (isFilled(preRelease)) {
                sb.append(PRE_RELEASE_PREFIX).append(preRelease);
            }
            if (isFilled(build)) {
                sb.append(BUILD_PREFIX).append(build);
            }
            return VersionParser.parseValidSemVer(sb.toString());
        }

        /**
         * Verifica se uma string tem um valor utilizável.
         *
         * @param str a string para verificar
         * @return {@code true} se a string for preenchida ou {@code false} caso contrário
         */
        private boolean isFilled(String str) {
            return str != null && !str.isEmpty();
        }
    }

    /**
     * Um comparador com reconhecimento de construção.
     */
    private static class BuildAwareOrder implements Comparator<Version> {

        /**
         * Compara duas instâncias de {@code Version} tomando
         * em consideração seus metadados de construção.
         * <p>
         * Quando os metadados de compilação comparados são divididos em identificadores. o
         * identificadores numéricos são comparados numericamente, e os alfanuméricos
         * identificadores são comparados na ordem de classificação ASCII.
         * <p>
         * Se uma das versões comparadas não tiver uma construção definida
         * metadados, esta versão é considerada como tendo um menor
         * precedência do que o outro.
         *
         * @return {@inheritDoc}
         */
        @Override
        public int compare(Version v1, Version v2) {
            int result = v1.compareTo(v2);
            if (result == 0) {
                result = v1.build.compareTo(v2.build);
                if (v1.build == MetadataVersion.metadataNull ||
                        v2.build == MetadataVersion.metadataNull
                ) {
                    /**
                     * Os metadados de compilação devem ter uma precedência mais alta
                     * do que a versão normal associada que é o
                     * oposto em comparação com as versões de pré-lançamento.
                     */
                    result = -1 * result;
                }
            }
            return result;
        }
    }
}
