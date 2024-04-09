package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.plugin.TestExtension;
import br.com.archbase.plugin.manager.plugin.TestExtensionPoint;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;


class AbstractArchbaseArchbasePluginManagerTest {

    @Test
    void getExtensionsByType() {
        AbstractArchbasePluginManager pluginManager = mock(AbstractArchbasePluginManager.class, CALLS_REAL_METHODS);

        ExtensionFinder extensionFinder = mock(ExtensionFinder.class);
        List<ExtensionWrapper<TestExtensionPoint>> extensionList = new ArrayList<>(1);
        extensionList.add(new ExtensionWrapper<>(new ExtensionDescriptor(0, TestExtension.class), new DefaultArchbaseExtensionFactory()));
        when(extensionFinder.find(TestExtensionPoint.class)).thenReturn(extensionList);

        pluginManager.extensionFinder = extensionFinder;
        List<TestExtensionPoint> extensions = pluginManager.getExtensions(TestExtensionPoint.class);
        assertEquals(1, extensions.size());
    }

}
