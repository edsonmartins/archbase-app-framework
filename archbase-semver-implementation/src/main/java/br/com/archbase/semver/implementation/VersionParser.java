package br.com.archbase.semver.implementation;

import br.com.archbase.semver.implementation.util.Stream;
import br.com.archbase.semver.implementation.util.UnexpectedElementException;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static br.com.archbase.semver.implementation.VersionParser.CharType.*;

/**
 * Um analisador para a versão SemVer.
 */
public class VersionParser implements Parser<Version> {

    /**
     * O fluxo de caracteres.
     */
    private final Stream<Character> chars;

    /**
     * Constrói uma instância {@code VersionParser}
     * com a string de entrada para analisar.
     *
     * @param input a string de entrada para analisar
     * @throws IllegalArgumentException se a string de entrada for {@code NULL} ou vazia
     */
    VersionParser(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("A string de entrada é NULL ou vazia");
        }
        Character[] elements = new Character[input.length()];
        for (int i = 0; i < input.length(); i++) {
            elements[i] = input.charAt(i);
        }
        chars = new Stream<>(elements);
    }

    /**
     * Analisa toda a versão, incluindo a versão de pré-lançamento e metadados de compilação.
     *
     * @param version a string de versão a ser analisada
     * @return um objeto de versão válida
     * @throws IllegalArgumentException     se a string de entrada for {@code NULL} ou vazia
     * @throws ParseException               quando há um erro de gramática
     * @throws UnexpectedCharacterException quando encontra um tipo de caractere inesperado
     */
    static Version parseValidSemVer(String version) {
        VersionParser parser = new VersionParser(version);
        return parser.parseValidSemVer();
    }

    /**
     * Analisa o núcleo da versão.
     *
     * @param versionCore a string principal da versão para analisar
     * @return um objeto de versão normal válido
     * @throws IllegalArgumentException     se a string de entrada for {@code NULL} ou vazia
     * @throws ParseException               quando há um erro de gramática
     * @throws UnexpectedCharacterException quando encontra um tipo de caractere inesperado
     */
    static NormalVersion parseVersionCore(String versionCore) {
        VersionParser parser = new VersionParser(versionCore);
        return parser.parseVersionCore();
    }

    /**
     * Analisa a versão de pré-lançamento.
     *
     * @param preRelease a string da versão de pré-lançamento para analisar
     * @return um objeto de versão de pré-lançamento válido
     * @throws IllegalArgumentException     se a string de entrada for {@code NULL} ou vazia
     * @throws ParseException               quando há um erro de gramática
     * @throws UnexpectedCharacterException quando encontra um tipo de caractere inesperado
     */
    static MetadataVersion parsePreRelease(String preRelease) {
        VersionParser parser = new VersionParser(preRelease);
        return parser.parsePreRelease();
    }

    /**
     * Analisa os metadados de construção.
     *
     * @param build a string de metadados de construção para analisar
     * @return um objeto de metadados de construção válido
     * @throws IllegalArgumentException     se a string de entrada for {@code NULL} ou vazia
     * @throws ParseException               quando há um erro de gramática
     * @throws UnexpectedCharacterException quando encontra um tipo de caractere inesperado
     */
    static MetadataVersion parseBuild(String build) {
        VersionParser parser = new VersionParser(build);
        return parser.parseBuild();
    }

    /**
     * Analisa a string de entrada.
     *
     * @param input a string de entrada para analisar
     * @return um objeto de versão válida
     * @throws ParseException               quando há um erro de gramática
     * @throws UnexpectedCharacterException quando encontra um tipo de caractere inesperado
     */
    @Override
    public Version parse(String input) {
        return parseValidSemVer();
    }

    /**
     * Analisa o não terminal {@literal <valid semver>}.
     *
     * <pre>
     * {@literal
     * <valid semver> ::= <version core>
     *                  | <version core> "-" <pre-release>
     *                  | <version core> "+" <build>
     *                  | <version core> "-" <pre-release> "+" <build>
     * }
     * </pre>
     *
     * @return um objeto de versão válida
     */
    private Version parseValidSemVer() {
        NormalVersion normal = parseVersionCore();
        MetadataVersion preRelease = MetadataVersion.metadataNull;
        MetadataVersion build = MetadataVersion.metadataNull;

        Character next = consumeNextCharacter(HYPHEN, PLUS, EOI);
        if (HYPHEN.isMatchedBy(next)) {
            preRelease = parsePreRelease();
            next = consumeNextCharacter(PLUS, EOI);
            if (PLUS.isMatchedBy(next)) {
                build = parseBuild();
            }
        } else if (PLUS.isMatchedBy(next)) {
            build = parseBuild();
        }
        consumeNextCharacter(EOI);
        return new Version(normal, preRelease, build);
    }

    /**
     * Analisa o não terminal {@literal <version core>}.
     *
     * <pre>
     * {@literal
     * <version core> ::= <major> "." <minor> "." <patch>
     * }
     * </pre>
     *
     * @return um objeto de versão normal válido
     */
    private NormalVersion parseVersionCore() {
        int major = Integer.parseInt(numericIdentifier());
        consumeNextCharacter(DOT);
        int minor = Integer.parseInt(numericIdentifier());
        consumeNextCharacter(DOT);
        int patch = Integer.parseInt(numericIdentifier());
        return new NormalVersion(major, minor, patch);
    }

    /**
     * Analisa o não terminal {@literal <pre-release>}.
     *
     * <pre>
     * {@literal
     * <pre-release> ::= <dot-separated pre-release identifiers>
     *
     * <dot-separated pre-release identifiers> ::= <pre-release identifier>
     *    | <pre-release identifier> "." <dot-separated pre-release identifiers>
     * }
     * </pre>
     *
     * @return um objeto de versão de pré-lançamento válido
     */
    private MetadataVersion parsePreRelease() {
        ensureValidLookahead(DIGIT, LETTER, HYPHEN);
        List<String> idents = new ArrayList<>();
        do {
            idents.add(preReleaseIdentifier());
            if (chars.positiveLookahead(DOT)) {
                consumeNextCharacter(DOT);
            } else {
                break;
            }
        } while (true);
        return new MetadataVersion(idents.toArray(new String[idents.size()]));
    }

    /**
     * Analisa o não terminal {@literal <identificador de pré-lançamento>}.
     *
     * <pre>
     * {@literal
     * <pre-release identifier> ::= <alphanumeric identifier>
     *                            | <numeric identifier>
     * }
     * </pre>
     *
     * @return um único identificador de pré-lançamento
     */
    private String preReleaseIdentifier() {
        checkForEmptyIdentifier();
        CharType boundary = nearestCharType(DOT, PLUS, EOI);
        if (chars.positiveLookaheadBefore(boundary, LETTER, HYPHEN)) {
            return alphanumericIdentifier();
        } else {
            return numericIdentifier();
        }
    }

    /**
     * Analisa o não terminal {@literal <build>}.
     *
     * <pre>
     * {@literal
     * <build> ::= <dot-separated build identifiers>
     *
     * <dot-separated build identifiers> ::= <build identifier>
     *                | <build identifier> "." <dot-separated build identifiers>
     * }
     * </pre>
     *
     * @return um objeto de metadados de construção válido
     */
    private MetadataVersion parseBuild() {
        ensureValidLookahead(DIGIT, LETTER, HYPHEN);
        List<String> idents = new ArrayList<>();
        do {
            idents.add(buildIdentifier());
            if (chars.positiveLookahead(DOT)) {
                consumeNextCharacter(DOT);
            } else {
                break;
            }
        } while (true);
        return new MetadataVersion(idents.toArray(new String[idents.size()]));
    }

    /**
     * Analisa o não terminal {@literal <build identifier>}.
     *
     * <pre>
     * {@literal
     * <build identifier> ::= <alphanumeric identifier>
     *                      | <digits>
     * }
     * </pre>
     *
     * @return um único identificador de construção
     */
    private String buildIdentifier() {
        checkForEmptyIdentifier();
        CharType boundary = nearestCharType(DOT, EOI);
        if (chars.positiveLookaheadBefore(boundary, LETTER, HYPHEN)) {
            return alphanumericIdentifier();
        } else {
            return digits();
        }
    }

    /**
     * Analisa o não terminal {@literal <numeric identifier>}.
     *
     * <pre>
     * {@literal
     * <numeric identifier> ::= "0"
     *                        | <positive digit>
     *                        | <positive digit> <digits>
     * }
     * </pre>
     *
     * @return uma string que representa o identificador numérico
     */
    private String numericIdentifier() {
        checkForLeadingZeroes();
        return digits();
    }

    /**
     * Analisa o não terminal {@literal <alfanumérico>}.
     *
     * <pre>
     * {@literal
     * <alphanumeric identifier> ::= <non-digit>
     *             | <non-digit> <identifier characters>
     *             | <identifier characters> <non-digit>
     *             | <identifier characters> <non-digit> <identifier characters>
     * }
     * </pre>
     *
     * @return uma string que representa o identificador alfanumérico
     */
    private String alphanumericIdentifier() {
        StringBuilder sb = new StringBuilder();
        do {
            sb.append(consumeNextCharacter(DIGIT, LETTER, HYPHEN));
        } while (chars.positiveLookahead(DIGIT, LETTER, HYPHEN));
        return sb.toString();
    }

    /**
     * Analisa o não terminal {@literal <digits>}.
     *
     * <pre>
     * {@literal
     * <digits> ::= <digit>
     *            | <digit> <digits>
     * }
     * </pre>
     *
     * @return a string representing the digits
     */
    private String digits() {
        StringBuilder sb = new StringBuilder();
        do {
            sb.append(consumeNextCharacter(DIGIT));
        } while (chars.positiveLookahead(DIGIT));
        return sb.toString();
    }

    /**
     * Encontra o tipo de caractere mais próximo.
     *
     * @param types os tipos de caracteres para escolher
     * @return o tipo de caractere mais próximo ou {@code EOI}
     */
    private CharType nearestCharType(CharType... types) {
        for (Character chr : chars) {
            for (CharType type : types) {
                if (type.isMatchedBy(chr)) {
                    return type;
                }
            }
        }
        return EOI;
    }

    /**
     * Verifica zeros à esquerda nos identificadores numéricos.
     *
     * @throws ParseException se um identificador numérico tiver zeros à esquerda
     */
    private void checkForLeadingZeroes() {
        Character la1 = chars.lookahead(1);
        Character la2 = chars.lookahead(2);
        if (la1 != null && la1 == '0' && DIGIT.isMatchedBy(la2)) {
            throw new ParseException(
                    "O identificador numérico NÃO DEVE conter zeros à esquerda"
            );
        }
    }

    /**
     * Verifica se há identificadores vazios na versão de pré-lançamento ou metadados de compilação.
     *
     * @throws ParseException se a versão ou compilação de pré-lançamento
     *                        metadados têm identificador (es) vazio (s)
     */
    private void checkForEmptyIdentifier() {
        Character la = chars.lookahead(1);
        if (DOT.isMatchedBy(la) || PLUS.isMatchedBy(la) || EOI.isMatchedBy(la)) {
            throw new ParseException(
                    "Os identificadores NÃO DEVEM estar vazios",
                    new UnexpectedCharacterException(
                            la,
                            chars.currentOffset(),
                            DIGIT, LETTER, HYPHEN
                    )
            );
        }
    }

    /**
     * Tenta consumir o próximo caracter no fluxo.
     *
     * @param expected os tipos esperados do próximo caractere
     * @return o próximo caractere no stream
     * @throws UnexpectedCharacterException quando encontra um tipo de caractere inesperado
     */
    private Character consumeNextCharacter(CharType... expected) {
        try {
            return chars.consume(expected);
        } catch (UnexpectedElementException e) {
            throw new UnexpectedCharacterException(e);
        }
    }

    /**
     * Verifica se o próximo caracter no stream é válido.
     *
     * @param expected os tipos esperados do próximo caractere
     * @throws UnexpectedCharacterException se o próximo caractere não for válido
     */
    private void ensureValidLookahead(CharType... expected) {
        if (!chars.positiveLookahead(expected)) {
            throw new UnexpectedCharacterException(
                    chars.lookahead(1),
                    chars.currentOffset(),
                    expected
            );
        }
    }

    /**
     * Tipos de caracteres válidos.
     */
    enum CharType implements Stream.ElementType<Character> {

        DIGIT {
            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isMatchedBy(Character chr) {
                if (chr == null) {
                    return false;
                }
                return chr >= '0' && chr <= '9';
            }
        },
        LETTER {
            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isMatchedBy(Character chr) {
                if (chr == null) {
                    return false;
                }
                return (chr >= 'a' && chr <= 'z')
                        || (chr >= 'A' && chr <= 'Z');
            }
        },
        DOT {
            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isMatchedBy(Character chr) {
                if (chr == null) {
                    return false;
                }
                return chr == '.';
            }
        },
        HYPHEN {
            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isMatchedBy(Character chr) {
                if (chr == null) {
                    return false;
                }
                return chr == '-';
            }
        },
        PLUS {
            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isMatchedBy(Character chr) {
                if (chr == null) {
                    return false;
                }
                return chr == '+';
            }
        },
        EOI {
            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isMatchedBy(Character chr) {
                return chr == null;
            }
        },
        ILLEGAL {
            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isMatchedBy(Character chr) {
                EnumSet<CharType> itself = EnumSet.of(ILLEGAL);
                for (CharType type : EnumSet.complementOf(itself)) {
                    if (type.isMatchedBy(chr)) {
                        return false;
                    }
                }
                return true;
            }
        };

        /**
         * Obtém o tipo de um determinado caractere.
         *
         * @param chr o caractere para obter o tipo para
         * @return o tipo do caractere especificado
         */
        static CharType forCharacter(Character chr) {
            for (CharType type : values()) {
                if (type.isMatchedBy(chr)) {
                    return type;
                }
            }
            return null;
        }
    }
}
