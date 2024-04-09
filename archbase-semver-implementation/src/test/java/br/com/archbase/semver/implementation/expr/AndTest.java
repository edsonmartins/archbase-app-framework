package br.com.archbase.semver.implementation.expr;

import br.com.archbase.semver.implementation.Version;
import org.junit.Test;

import static org.junit.Assert.assertTrue;


public class AndTest {

    @Test
    public void shouldCheckIfBothExpressionsEvaluateToTrue() {
        Expression left = new Expression() {
            @Override
            public boolean interpret(Version version) {
                return true;
            }
        };
        Expression right = new Expression() {
            @Override
            public boolean interpret(Version version) {
                return true;
            }
        };
        And and = new And(left, right);
        assertTrue(and.interpret(null));
    }
}
