package br.com.archbase.semver.implementation.expr;

import br.com.archbase.semver.implementation.Version;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class GreaterTest {

    @Test
    public void shouldCheckIfVersionIsGreaterThanParsedVersion() {
        Version parsed = Version.valueOf("2.0.0");
        Greater gt = new Greater(parsed);
        assertTrue(gt.interpret(Version.valueOf("3.2.1")));
        assertFalse(gt.interpret(Version.valueOf("1.2.3")));
    }
}
