package br.com.archbase.plugin.manager.update;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertTrue;

/**
 * Teste o pluginInfo
 */
public class ArchbasePluginInfoTest {

    private PluginInfo pi1;
    private PluginInfo pi2;

    @Before
    public void setup() {
        pi1 = new PluginInfo();
        pi1.id = "pi1";
        PluginInfo.PluginRelease pi1r1 = new PluginInfo.PluginRelease();
        pi1r1.version = "1.0.0";
        pi1r1.requires = "2";
        PluginInfo.PluginRelease pi1r2 = new PluginInfo.PluginRelease();
        pi1r2.version = "1.1.0";
        pi1r2.requires = "~2.1";
        PluginInfo.PluginRelease pi1r3 = new PluginInfo.PluginRelease();
        pi1r3.version = "1.2.0";
        pi1r3.requires = ">2.5 & < 4";
        pi1.releases = Arrays.asList(pi1r1, pi1r2, pi1r3);
        pi2 = new PluginInfo();
        PluginInfo.PluginRelease pi2r1 = new PluginInfo.PluginRelease();
        pi2r1.version = "1.0.0";
        pi2.id = "aaa";
        pi2.releases = Collections.singletonList(pi2r1);
    }

    @Test
    public void comparePluginInfo() {
        assertTrue(pi1.compareTo(pi2) > 0);
    }

}