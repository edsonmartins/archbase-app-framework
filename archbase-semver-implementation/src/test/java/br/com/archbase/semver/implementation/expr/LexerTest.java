package br.com.archbase.semver.implementation.expr;

import br.com.archbase.semver.implementation.expr.Lexer.Token;
import br.com.archbase.semver.implementation.util.Stream;
import org.junit.Test;

import static br.com.archbase.semver.implementation.expr.Lexer.Token.Type.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;


public class LexerTest {

    @Test
    public void shouldTokenizeVersionString() {
        Token[] expected = {
                new Token(GREATER, ">", 0),
                new Token(NUMERIC, "1", 1),
                new Token(DOT, ".", 2),
                new Token(NUMERIC, "0", 3),
                new Token(DOT, ".", 4),
                new Token(NUMERIC, "0", 5),
                new Token(EOI, null, 6),
        };
        Lexer lexer = new Lexer();
        Stream<Token> stream = lexer.tokenize(">1.0.0");
        assertArrayEquals(expected, stream.toArray());
    }

    @Test
    public void shouldSkipWhitespaces() {
        Token[] expected = {
                new Token(GREATER, ">", 0),
                new Token(NUMERIC, "1", 2),
                new Token(EOI, null, 3),
        };
        Lexer lexer = new Lexer();
        Stream<Token> stream = lexer.tokenize("> 1");
        assertArrayEquals(expected, stream.toArray());
    }

    @Test
    public void shouldEndWithEol() {
        Token[] expected = {
                new Token(NUMERIC, "1", 0),
                new Token(DOT, ".", 1),
                new Token(NUMERIC, "2", 2),
                new Token(DOT, ".", 3),
                new Token(NUMERIC, "3", 4),
                new Token(EOI, null, 5),
        };
        Lexer lexer = new Lexer();
        Stream<Token> stream = lexer.tokenize("1.2.3");
        assertArrayEquals(expected, stream.toArray());
    }

    @Test
    public void shouldRaiseErrorOnIllegalCharacter() {
        Lexer lexer = new Lexer();
        try {
            lexer.tokenize("@1.0.0");
        } catch (LexerException e) {
            return;
        }
        fail("Deve levantar erro em caráter ilegal");
    }
}
