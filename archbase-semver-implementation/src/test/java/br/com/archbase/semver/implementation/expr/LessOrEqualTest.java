package br.com.archbase.semver.implementation.expr;

import br.com.archbase.semver.implementation.Version;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class LessOrEqualTest {

    @Test
    public void shouldCheckIfVersionIsLessThanOrEqualToParsedVersion() {
        Version parsed = Version.valueOf("2.0.0");
        LessOrEqual le = new LessOrEqual(parsed);
        assertTrue(le.interpret(Version.valueOf("1.2.3")));
        assertTrue(le.interpret(Version.valueOf("2.0.0")));
        assertFalse(le.interpret(Version.valueOf("3.2.1")));
    }
}
