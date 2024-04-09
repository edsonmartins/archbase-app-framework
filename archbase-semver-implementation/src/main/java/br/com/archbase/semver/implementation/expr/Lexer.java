package br.com.archbase.semver.implementation.expr;

import br.com.archbase.semver.implementation.util.Stream;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A lexer for the SemVer Expressions.
 */
public class Lexer {

    /**
     * Constrói uma instância {@code Lexer}.
     */
    Lexer() {

    }

    /**
     * Tokeniza a string de entrada especificada.
     *
     * @param input a string de entrada para tokenizar
     * @return um fluxo de tokens
     * @throws LexerException quando encontra um personagem ilegal
     */
    Stream<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();
        int tokenPos = 0;
        while (!input.isEmpty()) {
            boolean matched = false;
            for (Token.Type tokenType : Token.Type.values()) {
                Matcher matcher = tokenType.pattern.matcher(input);
                if (matcher.find()) {
                    matched = true;
                    input = matcher.replaceFirst("");
                    if (tokenType != Token.Type.WHITESPACE) {
                        tokens.add(new Token(
                                tokenType,
                                matcher.group(),
                                tokenPos
                        ));
                    }
                    tokenPos += matcher.end();
                    break;
                }
            }
            if (!matched) {
                throw new LexerException(input);
            }
        }
        tokens.add(new Token(Token.Type.EOI, null, tokenPos));
        return new Stream<>(tokens.toArray(new Token[tokens.size()]));
    }

    /**
     * This class holds the information about lexemes in the input stream.
     */
    static class Token {

        /**
         * O tipo deste token.
         */
        final Type type;
        /**
         * O lexema deste token.
         */
        final String lexeme;
        /**
         * A posição deste token.
         */
        final int position;

        /**
         * Constrói uma instância {@code Token}
         * com o tipo, lexema e posição.
         *
         * @param type     o tipo deste token
         * @param lexeme   o lexema deste token
         * @param position a posição deste token
         */
        Token(Type type, String lexeme, int position) {
            this.type = type;
            this.lexeme = (lexeme == null) ? "" : lexeme;
            this.position = position;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Token)) {
                return false;
            }
            Token token = (Token) other;
            return
                    type.equals(token.type) &&
                            lexeme.equals(token.lexeme) &&
                            position == token.position;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            int hash = 5;
            hash = 71 * hash + type.hashCode();
            hash = 71 * hash + lexeme.hashCode();
            hash = 71 * hash + position;
            return hash;
        }

        /**
         * Retorna a representação de string deste token.
         *
         * @return a representação de string deste token
         */
        @Override
        public String toString() {
            return String.format(
                    "%s(%s) at position %d",
                    type.name(),
                    lexeme, position
            );
        }

        /**
         * Tipos de token válidos.
         */
        enum Type implements Stream.ElementType<Token> {

            NUMERIC("0|[1-9][0-9]*"),
            DOT("\\."),
            HYPHEN("-"),
            EQUAL("="),
            NOT_EQUAL("!="),
            GREATER(">(?!=)"),
            GREATER_EQUAL(">="),
            LESS("<(?!=)"),
            LESS_EQUAL("<="),
            TILDE("~"),
            WILDCARD("[\\*xX]"),
            CARET("\\^"),
            AND("&"),
            OR("\\|"),
            NOT("!(?!=)"),
            LEFT_PAREN("\\("),
            RIGHT_PAREN("\\)"),
            WHITESPACE("\\s+"),
            EOI("?!");

            /**
             * A pattern matching this type.
             */
            final Pattern pattern;

            /**
             * Constrói um tipo de token com um
             * expressão para o padrão.
             *
             * @param regexp a expressão regular para o padrão
             * @see #pattern
             */
            private Type(String regexp) {
                pattern = Pattern.compile("^(" + regexp + ")");
            }

            /**
             * Retorna a representação de string deste tipo.
             *
             * @return a representação de string deste tipo
             */
            @Override
            public String toString() {
                return name() + "(" + pattern + ")";
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isMatchedBy(Token token) {
                if (token == null) {
                    return false;
                }
                return this == token.type;
            }
        }
    }
}
