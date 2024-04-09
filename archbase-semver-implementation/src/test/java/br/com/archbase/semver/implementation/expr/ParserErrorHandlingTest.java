package br.com.archbase.semver.implementation.expr;

import br.com.archbase.semver.implementation.expr.Lexer.Token;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

import static br.com.archbase.semver.implementation.expr.Lexer.Token.Type.*;
import static org.junit.Assert.*;


@RunWith(Parameterized.class)
public class ParserErrorHandlingTest {

    private final String invalidExpr;
    private final Token unexpected;
    private final Token.Type[] expected;

    public ParserErrorHandlingTest(
            String invalidExpr,
            Token unexpected,
            Token.Type[] expected
    ) {
        this.invalidExpr = invalidExpr;
        this.unexpected = unexpected;
        this.expected = expected;
    }

    @Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"1)", new Token(RIGHT_PAREN, ")", 1), new Token.Type[]{EOI}},
                {"(>1.0.1", new Token(EOI, null, 7), new Token.Type[]{RIGHT_PAREN}},
                {"((>=1 & <2)", new Token(EOI, null, 11), new Token.Type[]{RIGHT_PAREN}},
                {">=1.0.0 &", new Token(EOI, null, 9), new Token.Type[]{NUMERIC}},
                {"(>2.0 |)", new Token(RIGHT_PAREN, ")", 7), new Token.Type[]{NUMERIC}},
                {"& 1.2", new Token(AND, "&", 0), new Token.Type[]{NUMERIC}},
        });
    }

    @Test
    public void shouldCorrectlyHandleParseErrors() {
        try {
            ExpressionParser.newInstance().parse(invalidExpr);
        } catch (UnexpectedTokenException e) {
            assertEquals(unexpected, e.getUnexpectedToken());
            assertArrayEquals(expected, e.getExpectedTokenTypes());
            return;
        }
        fail("Exceção não capturada");
    }
}
