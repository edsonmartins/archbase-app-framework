package br.com.archbase.semver.implementation.expr;

import br.com.archbase.semver.implementation.expr.Lexer.Token;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static br.com.archbase.semver.implementation.expr.Lexer.Token.Type.*;
import static org.junit.Assert.*;


@RunWith(Enclosed.class)
@SuppressWarnings("java:S5785")
public class LexerTokenTest {

    public static class EqualsMethodTest {

        @Test
        public void shouldBeReflexive() {
            Token token = new Token(NUMERIC, "1", 0);
            assertTrue(token.equals(token));
        }

        @Test
        public void shouldBeSymmetric() {
            Token t1 = new Token(EQUAL, "=", 0);
            Token t2 = new Token(EQUAL, "=", 0);
            assertTrue(t1.equals(t2));
            assertTrue(t2.equals(t1));
        }

        @Test
        public void shouldBeTransitive() {
            Token t1 = new Token(GREATER, ">", 0);
            Token t2 = new Token(GREATER, ">", 0);
            Token t3 = new Token(GREATER, ">", 0);
            assertTrue(t1.equals(t2));
            assertTrue(t2.equals(t3));
            assertTrue(t1.equals(t3));
        }

        @Test
        public void shouldBeConsistent() {
            Token t1 = new Token(HYPHEN, "-", 0);
            Token t2 = new Token(HYPHEN, "-", 0);
            assertTrue(t1.equals(t2));
            assertTrue(t1.equals(t2));
            assertTrue(t1.equals(t2));
        }

        @Test
        public void shouldReturnFalseIfOtherVersionIsOfDifferentType() {
            Token t1 = new Token(DOT, ".", 0);
            assertFalse(t1.equals(new String(".")));
        }

        @Test
        public void shouldReturnFalseIfOtherVersionIsNull() {
            Token t1 = new Token(AND, "&", 0);
            Token t2 = null;
            assertFalse(t1.equals(t2));
        }

        @Test
        public void shouldReturnFalseIfTypesAreDifferent() {
            Token t1 = new Token(EQUAL, "=", 0);
            Token t2 = new Token(NOT_EQUAL, "!=", 0);
            assertFalse(t1.equals(t2));
        }

        @Test
        public void shouldReturnFalseIfLexemesAreDifferent() {
            Token t1 = new Token(NUMERIC, "1", 0);
            Token t2 = new Token(NUMERIC, "2", 0);
            assertFalse(t1.equals(t2));
        }

        @Test
        public void shouldReturnFalseIfPositionsAreDifferent() {
            Token t1 = new Token(NUMERIC, "1", 1);
            Token t2 = new Token(NUMERIC, "1", 2);
            assertFalse(t1.equals(t2));
        }
    }

    public static class HashCodeMethodTest {

        @Test
        public void shouldReturnSameHashCodeIfTokensAreEqual() {
            Token t1 = new Token(NUMERIC, "1", 0);
            Token t2 = new Token(NUMERIC, "1", 0);
            assertTrue(t1.equals(t2));
            assertEquals(t1.hashCode(), t2.hashCode());
        }
    }
}
