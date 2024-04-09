package br.com.archbase.semver.implementation.expr;

import br.com.archbase.semver.implementation.Version;
import org.junit.Test;

import static org.junit.Assert.assertTrue;


public class OrTest {

    @Test
    public void shouldCheckIfOneOfTwoExpressionsEvaluateToTrue() {
        Expression left = new Expression() {
            @Override
            public boolean interpret(Version version) {
                return false;
            }
        };
        Expression right = new Expression() {
            @Override
            public boolean interpret(Version version) {
                return true;
            }
        };
        Or or = new Or(left, right);
        assertTrue(or.interpret(null));
    }
}
