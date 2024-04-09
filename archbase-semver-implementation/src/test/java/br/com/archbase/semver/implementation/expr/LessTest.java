package br.com.archbase.semver.implementation.expr;

import br.com.archbase.semver.implementation.Version;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class LessTest {

    @Test
    public void shouldCheckIfVersionIsLessThanParsedVersion() {
        Version parsed = Version.valueOf("2.0.0");
        Less lt = new Less(parsed);
        assertTrue(lt.interpret(Version.valueOf("1.2.3")));
        assertFalse(lt.interpret(Version.valueOf("3.2.1")));
    }
}
