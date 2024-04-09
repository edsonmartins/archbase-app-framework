package br.com.archbase.plugin.manager;


import br.com.archbase.semver.implementation.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


class DefaultArchbaseVersionManagerTest {

    private VersionManager versionManager;

    @BeforeEach
    void init() {
        versionManager = new DefaultArchbaseVersionManager();
    }

    @Test
    void checkVersionConstraint() {
        assertFalse(versionManager.checkVersionConstraint("1.4.3", ">2.0.0")); // simple
        assertTrue(versionManager.checkVersionConstraint("1.4.3", ">=1.4.0 & <1.6.0")); // range
    }

    @Test
    void nullOrEmptyVersion() {
        assertThrows(IllegalArgumentException.class, () -> versionManager.checkVersionConstraint(null, ">2.0.0"));
    }

    @Test
    void invalidVersion() {
        assertThrows(ParseException.class, () -> versionManager.checkVersionConstraint("1.0", ">2.0.0"));
    }

    @Test
    void compareVersions() {
        assertTrue(versionManager.compareVersions("1.1.0", "1.0.0") > 0);
    }

}
