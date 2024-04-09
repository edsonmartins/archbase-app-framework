package br.com.archbase.semver.implementation.expr;

import br.com.archbase.semver.implementation.Parser;
import br.com.archbase.semver.implementation.Version;
import br.com.archbase.semver.implementation.expr.Lexer.Token;
import br.com.archbase.semver.implementation.util.Stream;
import br.com.archbase.semver.implementation.util.UnexpectedElementException;

import java.util.EnumSet;
import java.util.Iterator;

import static br.com.archbase.semver.implementation.expr.CompositeExpression.Helper.*;
import static br.com.archbase.semver.implementation.expr.Lexer.Token.Type.*;

/**
 * Um analisador para as expressões SemVer.
 */
public class ExpressionParser implements Parser<Expression> {

    /**
     * A instância lexer usada para tokenização da string de entrada.
     */
    private final Lexer lexer;

    /**
     * O fluxo de tokens produzidos pelo lexer.
     */
    private Stream<Token> tokens;

    /**
     * Constrói uma instância {@code ExpressionParser}
     * com o lexer correspondente.
     *
     * @param lexer o lexer a ser usado para tokenização da string de entrada
     */
    ExpressionParser(Lexer lexer) {
        this.lexer = lexer;
    }

    /**
     * Cria e retorna uma nova instância da classe {@code ExpressionParser}.
     * <p>
     * Este método implementa o padrão Static Factory Method.
     *
     * @return uma nova instância da classe {@code ExpressionParser}
     */
    public static Parser<Expression> newInstance() {
        return new ExpressionParser(new Lexer());
    }

    /**
     * Analisa as expressões SemVer.
     *
     * @param input uma string que representa a expressão SemVer
     * @return o AST para as expressões SemVer
     * @throws LexerException           quando encontra um personagem ilegal
     * @throws UnexpectedTokenException quando consome um token de um tipo inesperado
     */
    @Override
    public Expression parse(String input) {
        tokens = lexer.tokenize(input);
        Expression expr = parseSemVerExpression();
        consumeNextToken(EOI);
        return expr;
    }

    /**
     * Analisa o não terminal {@literal <semver-expr>}.
     *
     * <pre>
     * {@literal
     * <semver-expr> ::= "(" <semver-expr> ")"
     *                 | "!" "(" <semver-expr> ")"
     *                 | <semver-expr> <more-expr>
     *                 | <range>
     * }
     * </pre>
     *
     * @return a expressão AST
     */
    private CompositeExpression parseSemVerExpression() {
        CompositeExpression expr;
        if (tokens.positiveLookahead(NOT)) {
            tokens.consume();
            consumeNextToken(LEFT_PAREN);
            expr = not(parseSemVerExpression());
            consumeNextToken(RIGHT_PAREN);
        } else if (tokens.positiveLookahead(LEFT_PAREN)) {
            consumeNextToken(LEFT_PAREN);
            expr = parseSemVerExpression();
            consumeNextToken(RIGHT_PAREN);
        } else {
            expr = parseRange();
        }
        return parseMoreExpressions(expr);
    }

    /**
     * Parses the {@literal <more-expr>} non-terminal.
     *
     * <pre>
     * {@literal
     * <more-expr> ::= <boolean-op> <semver-expr> | epsilon
     * }
     * </pre>
     *
     * @param expr a expressão à esquerda dos operadores lógicos
     * @return a expressão AST
     */
    private CompositeExpression parseMoreExpressions(CompositeExpression expr) {
        if (tokens.positiveLookahead(AND)) {
            tokens.consume();
            expr = expr.and(parseSemVerExpression());
        } else if (tokens.positiveLookahead(OR)) {
            tokens.consume();
            expr = expr.or(parseSemVerExpression());
        }
        return expr;
    }

    /**
     * Analisa o não terminal {@literal <range>}.
     *
     * <pre>
     * {@literal
     * <expr> ::= <comparison-range>
     *          | <wildcard-expr>
     *          | <tilde-range>
     *          | <caret-range>
     *          | <hyphen-range>
     *          | <partial-version-range>
     * }
     * </pre>
     *
     * @return a expressão AST
     */
    private CompositeExpression parseRange() {
        if (tokens.positiveLookahead(TILDE)) {
            return parseTildeRange();
        } else if (tokens.positiveLookahead(CARET)) {
            return parseCaretRange();
        } else if (isWildcardRange()) {
            return parseWildcardRange();
        } else if (isHyphenRange()) {
            return parseHyphenRange();
        } else if (isPartialVersionRange()) {
            return parsePartialVersionRange();
        }
        return parseComparisonRange();
    }

    /**
     * Analisa o não terminal {@literal <comparison-range>}.
     *
     * <pre>
     * {@literal
     * <comparison-range> ::= <comparison-op> <version> | <version>
     * }
     * </pre>
     *
     * @return a expressão AST
     */
    private CompositeExpression parseComparisonRange() {
        Token token = tokens.lookahead();
        CompositeExpression expr;
        switch (token.type) {
            case EQUAL:
                tokens.consume();
                expr = eq(parseVersion());
                break;
            case NOT_EQUAL:
                tokens.consume();
                expr = neq(parseVersion());
                break;
            case GREATER:
                tokens.consume();
                expr = gt(parseVersion());
                break;
            case GREATER_EQUAL:
                tokens.consume();
                expr = gte(parseVersion());
                break;
            case LESS:
                tokens.consume();
                expr = lt(parseVersion());
                break;
            case LESS_EQUAL:
                tokens.consume();
                expr = lte(parseVersion());
                break;
            default:
                expr = eq(parseVersion());
        }
        return expr;
    }

    /**
     * Analisa o não terminal {@literal <tilde-range>}.
     *
     * <pre>
     * {@literal
     * <tilde-range> ::= "~" <version>
     * }
     * </pre>
     *
     * @return the expression AST
     */
    private CompositeExpression parseTildeRange() {
        consumeNextToken(TILDE);
        int major = intOf(consumeNextToken(NUMERIC).lexeme);
        if (!tokens.positiveLookahead(DOT)) {
            return gte(versionFor(major)).and(lt(versionFor(major + 1)));
        }
        consumeNextToken(DOT);
        int minor = intOf(consumeNextToken(NUMERIC).lexeme);
        if (!tokens.positiveLookahead(DOT)) {
            return gte(versionFor(major, minor)).and(lt(versionFor(major, minor + 1)));
        }
        consumeNextToken(DOT);
        int patch = intOf(consumeNextToken(NUMERIC).lexeme);
        return gte(versionFor(major, minor, patch)).and(lt(versionFor(major, minor + 1)));
    }

    /**
     * Parses the {@literal <caret-range>} non-terminal.
     *
     * <pre>
     * {@literal
     * <caret-range> ::= "^" <version>
     * }
     * </pre>
     *
     * @return a expressão AST
     */
    private CompositeExpression parseCaretRange() {
        consumeNextToken(CARET);
        int major = intOf(consumeNextToken(NUMERIC).lexeme);
        if (!tokens.positiveLookahead(DOT)) {
            return gte(versionFor(major)).and(lt(versionFor(major + 1)));
        }
        consumeNextToken(DOT);
        int minor = intOf(consumeNextToken(NUMERIC).lexeme);
        if (!tokens.positiveLookahead(DOT)) {
            Version lower = versionFor(major, minor);
            Version upper = major > 0 ? lower.incrementMajorVersion() : lower.incrementMinorVersion();
            return gte(lower).and(lt(upper));
        }
        consumeNextToken(DOT);
        int patch = intOf(consumeNextToken(NUMERIC).lexeme);
        Version version = versionFor(major, minor, patch);
        CompositeExpression gte = gte(version);
        if (major > 0) {
            return gte.and(lt(version.incrementMajorVersion()));
        } else if (minor > 0) {
            return gte.and(lt(version.incrementMinorVersion()));
        } else if (patch > 0) {
            return gte.and(lt(version.incrementPatchVersion()));
        }
        return eq(version);
    }

    /**
     * Determina se os terminais de versão a seguir fazem parte
     * do não terminal {@literal <wildcard-range>}.
     *
     * @return {@code true} se os terminais de versão a seguir forem
     * parte do {@literal <wildcard-range>} não terminal ou
     * {@code false} caso contrário
     */
    private boolean isWildcardRange() {
        return isVersionFollowedBy(WILDCARD);
    }

    /**
     * Analisa o não terminal {@literal <wildcard-range>}.
     *
     * <pre>
     * {@literal
     * <wildcard-range> ::= <wildcard>
     *                    | <major> "." <wildcard>
     *                    | <major> "." <minor> "." <wildcard>
     *
     * <wildcard> ::= "*" | "x" | "X"
     * }
     * </pre>
     *
     * @return a expressão AST
     */
    private CompositeExpression parseWildcardRange() {
        if (tokens.positiveLookahead(WILDCARD)) {
            tokens.consume();
            return gte(versionFor(0, 0, 0));
        }

        int major = intOf(consumeNextToken(NUMERIC).lexeme);
        consumeNextToken(DOT);
        if (tokens.positiveLookahead(WILDCARD)) {
            tokens.consume();
            return gte(versionFor(major)).and(lt(versionFor(major + 1)));
        }

        int minor = intOf(consumeNextToken(NUMERIC).lexeme);
        consumeNextToken(DOT);
        consumeNextToken(WILDCARD);
        return gte(versionFor(major, minor)).and(lt(versionFor(major, minor + 1)));
    }

    /**
     * Determina se os terminais de versão a seguir são
     * parte do {@literal <hyphen-range>} não terminal.
     *
     * @return {@code true} se os terminais de versão a seguir forem
     * parte do {@literal <hyphen-range>} não terminal ou
     * {@code false} caso contrário
     */
    private boolean isHyphenRange() {
        return isVersionFollowedBy(HYPHEN);
    }

    /**
     * Analisa o não terminal {@literal <hyphen-range>}.
     *
     * <pre>
     * {@literal
     * <hyphen-range> ::= <version> "-" <version>
     * }
     * </pre>
     *
     * @return a expressão AST
     */
    private CompositeExpression parseHyphenRange() {
        CompositeExpression gte = gte(parseVersion());
        consumeNextToken(HYPHEN);
        return gte.and(lte(parseVersion()));
    }

    /**
     * Determina se os terminais de versão a seguir fazem parte
     * do {@literal <partial-version-range>} não terminal.
     *
     * @return {@code true} se os terminais de versão a seguir fizerem parte
     * do {@literal <partial-version-range>} não terminal ou
     * {@code false} caso contrário
     */
    private boolean isPartialVersionRange() {
        if (!tokens.positiveLookahead(NUMERIC)) {
            return false;
        }
        EnumSet<Token.Type> expected = EnumSet.complementOf(EnumSet.of(NUMERIC, DOT));
        return tokens.positiveLookaheadUntil(5, expected.toArray(new Token.Type[expected.size()]));
    }

    /**
     * Analisa o não terminal {@literal <partial-version-range>}.
     *
     * <pre>
     * {@literal
     * <partial-version-range> ::= <major> | <major> "." <minor>
     * }
     * </pre>
     *
     * @return a expressão AST
     */
    private CompositeExpression parsePartialVersionRange() {
        int major = intOf(consumeNextToken(NUMERIC).lexeme);
        if (!tokens.positiveLookahead(DOT)) {
            return gte(versionFor(major)).and(lt(versionFor(major + 1)));
        }
        consumeNextToken(DOT);
        int minor = intOf(consumeNextToken(NUMERIC).lexeme);
        return gte(versionFor(major, minor)).and(lt(versionFor(major, minor + 1)));
    }

    /**
     * Analisa o não terminal {@literal <version>}.
     *
     * <pre>
     * {@literal
     * <version> ::= <major>
     *             | <major> "." <minor>
     *             | <major> "." <minor> "." <patch>
     * }
     * </pre>
     *
     * @return a versão analisada
     */
    private Version parseVersion() {
        int major = intOf(consumeNextToken(NUMERIC).lexeme);
        int minor = 0;
        if (tokens.positiveLookahead(DOT)) {
            tokens.consume();
            minor = intOf(consumeNextToken(NUMERIC).lexeme);
        }
        int patch = 0;
        if (tokens.positiveLookahead(DOT)) {
            tokens.consume();
            patch = intOf(consumeNextToken(NUMERIC).lexeme);
        }
        return versionFor(major, minor, patch);
    }

    /**
     * Determina se os terminais de versão são
     * seguido pelo tipo de token especificado.
     * <p>
     * Este método é essencialmente um método {@code lookahead (k)}
     * que permite resolver as ambiguidades da gramática.
     *
     * @param type o tipo de token a ser verificado
     * @return {@code true} se os terminais de versão forem seguidos por
     * o tipo de token especificado ou {@code false} caso contrário
     */
    private boolean isVersionFollowedBy(Stream.ElementType<Token> type) {
        EnumSet<Token.Type> expected = EnumSet.of(NUMERIC, DOT);
        Iterator<Token> it = tokens.iterator();
        Token lookahead = null;
        while (it.hasNext()) {
            lookahead = it.next();
            if (!expected.contains(lookahead.type)) {
                break;
            }
        }
        return type.isMatchedBy(lookahead);
    }

    /**
     * Cria uma instância {@code Version} para a versão principal especificada.
     *
     * @param major o número da versão principal
     * @return a versão para a versão principal especificada
     */
    private Version versionFor(int major) {
        return versionFor(major, 0, 0);
    }

    /**
     * Cria uma instância {@code Version} para
     * as versões principais e secundárias especificadas.
     *
     * @param major o número da versão principal
     * @param minor o número da versão secundária
     * @return a versão para as versões principais e secundárias especificadas
     */
    private Version versionFor(int major, int minor) {
        return versionFor(major, minor, 0);
    }

    /**
     * Cria uma instância {@code Version} para o
     * versões principais, secundárias e de patch especificadas.
     *
     * @param major o número da versão principal
     * @param minor o número da versão secundária
     * @param patch o número da versão do patch
     * @return a versão para as versões principais, secundárias e de patch especificadas
     */
    private Version versionFor(int major, int minor, int patch) {
        return Version.forIntegers(major, minor, patch);
    }

    /**
     * Retorna uma representação {@code int} da string especificada.
     *
     * @param value a string a ser convertida em um inteiro
     * @return o valor inteiro da string especificada
     */
    private int intOf(String value) {
        return Integer.parseInt(value);
    }

    /**
     * Tenta consumir o próximo token no fluxo.
     *
     * @param expected os tipos esperados do próximo token
     * @return o próximo token no stream
     * @throws UnexpectedTokenException ao encontrar um tipo de token inesperado
     */
    private Token consumeNextToken(Token.Type... expected) {
        try {
            return tokens.consume(expected);
        } catch (UnexpectedElementException e) {
            throw new UnexpectedTokenException(e);
        }
    }
}
