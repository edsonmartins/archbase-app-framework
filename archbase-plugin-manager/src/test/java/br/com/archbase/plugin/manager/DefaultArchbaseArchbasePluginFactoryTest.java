package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.plugin.AnotherFailTestPlugin;
import br.com.archbase.plugin.manager.plugin.FailTestPlugin;
import br.com.archbase.plugin.manager.plugin.TestPlugin;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class DefaultArchbaseArchbasePluginFactoryTest {

    @Test
    void testCreate() {
        PluginDescriptor pluginDescriptor = mock(PluginDescriptor.class);
        when(pluginDescriptor.getPluginClass()).thenReturn(TestPlugin.class.getName());

        PluginWrapper pluginWrapper = mock(PluginWrapper.class);
        when(pluginWrapper.getDescriptor()).thenReturn(pluginDescriptor);
        when(pluginWrapper.getPluginClassLoader()).thenReturn(getClass().getClassLoader());

        ArchbasePluginFactory archbasePluginFactory = new DefaultArchbasePluginFactory();

        ArchbasePlugin result = archbasePluginFactory.create(pluginWrapper);
        assertNotNull(result);
        assertThat(result, instanceOf(TestPlugin.class));
    }

    @Test
    void testCreateFail() {
        PluginDescriptor pluginDescriptor = mock(PluginDescriptor.class);
        when(pluginDescriptor.getPluginClass()).thenReturn(FailTestPlugin.class.getName());

        PluginWrapper pluginWrapper = mock(PluginWrapper.class);
        when(pluginWrapper.getDescriptor()).thenReturn(pluginDescriptor);
        when(pluginWrapper.getPluginClassLoader()).thenReturn(getClass().getClassLoader());

        ArchbasePluginFactory archbasePluginFactory = new DefaultArchbasePluginFactory();

        ArchbasePlugin archbasePlugin = archbasePluginFactory.create(pluginWrapper);
        assertNull(archbasePlugin);
    }

    @Test
    void testCreateFailNotFound() {
        PluginDescriptor pluginDescriptor = mock(PluginDescriptor.class);
        when(pluginDescriptor.getPluginClass()).thenReturn("org.archbase.plugin.NotFoundTestPlugin");

        PluginWrapper pluginWrapper = mock(PluginWrapper.class);
        when(pluginWrapper.getDescriptor()).thenReturn(pluginDescriptor);
        when(pluginWrapper.getPluginClassLoader()).thenReturn(getClass().getClassLoader());

        ArchbasePluginFactory archbasePluginFactory = new DefaultArchbasePluginFactory();

        ArchbasePlugin archbasePlugin = archbasePluginFactory.create(pluginWrapper);
        assertNull(archbasePlugin);
    }

    @Test
    void testCreateFailConstructor() {
        PluginDescriptor pluginDescriptor = mock(PluginDescriptor.class);
        when(pluginDescriptor.getPluginClass()).thenReturn(AnotherFailTestPlugin.class.getName());

        PluginWrapper pluginWrapper = mock(PluginWrapper.class);
        when(pluginWrapper.getDescriptor()).thenReturn(pluginDescriptor);
        when(pluginWrapper.getPluginClassLoader()).thenReturn(getClass().getClassLoader());

        ArchbasePluginFactory archbasePluginFactory = new DefaultArchbasePluginFactory();

        ArchbasePlugin archbasePlugin = archbasePluginFactory.create(pluginWrapper);
        assertNull(archbasePlugin);
    }

}
