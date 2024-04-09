package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.plugin.FailTestPlugin;
import br.com.archbase.plugin.manager.plugin.TestExtensionPoint;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import com.google.testing.compile.Compilation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.*;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;


class AbstractExtensionFinderTest {

    private ArchbasePluginManager archbasePluginManager;

    @BeforeEach
    public void setUp() {
        PluginWrapper pluginStarted = mock(PluginWrapper.class);
        when(pluginStarted.getPluginClassLoader()).thenReturn(getClass().getClassLoader());
        when(pluginStarted.getPluginState()).thenReturn(PluginState.STARTED);

        PluginWrapper pluginStopped = mock(PluginWrapper.class);
        when(pluginStopped.getPluginClassLoader()).thenReturn(getClass().getClassLoader());
        when(pluginStopped.getPluginState()).thenReturn(PluginState.STOPPED);

        archbasePluginManager = mock(ArchbasePluginManager.class);
        when(archbasePluginManager.getPlugin(eq("plugin1"))).thenReturn(pluginStarted);
        when(archbasePluginManager.getPlugin(eq("plugin2"))).thenReturn(pluginStopped);
        when(archbasePluginManager.getPluginClassLoader(eq("plugin1"))).thenReturn(getClass().getClassLoader());
        when(archbasePluginManager.getExtensionFactory()).thenReturn(new DefaultArchbaseExtensionFactory());
    }

    @AfterEach
    public void tearDown() {
        archbasePluginManager = null;
    }

    /**
     * Teste de {@link AbstractExtensionFinder#find(Class)}.
     */
    @Test
    void testFindFailType() {
        ExtensionFinder instance = new AbstractExtensionFinder(archbasePluginManager) {

            @Override
            public Map<String, Set<String>> readPluginsStorages() {
                return Collections.emptyMap();
            }

            @Override
            public Map<String, Set<String>> readClasspathStorages() {
                return Collections.emptyMap();
            }

        };
        List<ExtensionWrapper<FailTestPlugin>> list = instance.find(FailTestPlugin.class);
        assertEquals(0, list.size());
    }

    /**
     * Teste de {@link AbstractExtensionFinder#find(Class)}.
     */
    @Test
    void testFindFromClasspath() {
        ExtensionFinder instance = new AbstractExtensionFinder(archbasePluginManager) {

            @Override
            public Map<String, Set<String>> readPluginsStorages() {
                return Collections.emptyMap();
            }

            @Override
            public Map<String, Set<String>> readClasspathStorages() {
                Map<String, Set<String>> entries = new LinkedHashMap<>();

                Set<String> bucket = new HashSet<>();
                bucket.add("br.com.archbase.plugin.manager.plugin.TestExtension");
                bucket.add("br.com.archbase.plugin.manager.plugin.FailTestExtension");
                entries.put(null, bucket);

                return entries;
            }

        };

        List<ExtensionWrapper<TestExtensionPoint>> list = instance.find(TestExtensionPoint.class);
        assertEquals(2, list.size());
    }

    /**
     * Teste de {@link AbstractExtensionFinder#find(Class, String)}.
     */
    @Test
    void testFindFromPlugin() {
        ExtensionFinder instance = new AbstractExtensionFinder(archbasePluginManager) {

            @Override
            public Map<String, Set<String>> readPluginsStorages() {
                Map<String, Set<String>> entries = new LinkedHashMap<>();

                Set<String> bucket = new HashSet<>();
                bucket.add("br.com.archbase.plugin.manager.plugin.TestExtension");
                bucket.add("br.com.archbase.plugin.manager.plugin.FailTestExtension");
                entries.put("plugin1", bucket);
                bucket = new HashSet<>();
                bucket.add("br.com.archbase.plugin.manager.plugin.TestExtension");
                entries.put("plugin2", bucket);

                return entries;
            }

            @Override
            public Map<String, Set<String>> readClasspathStorages() {
                return Collections.emptyMap();
            }

        };

        List<ExtensionWrapper<TestExtensionPoint>> list = instance.find(TestExtensionPoint.class);
        assertEquals(2, list.size());

        list = instance.find(TestExtensionPoint.class, "plugin1");
        assertEquals(2, list.size());

        list = instance.find(TestExtensionPoint.class, "plugin2");
        assertEquals(0, list.size());
    }

    /**
     * Teste de {@link AbstractExtensionFinder #findClassNames(String)}.
     */
    @Test
    void testFindClassNames() {
        ExtensionFinder instance = new AbstractExtensionFinder(archbasePluginManager) {

            @Override
            public Map<String, Set<String>> readPluginsStorages() {
                Map<String, Set<String>> entries = new LinkedHashMap<>();

                Set<String> bucket = new HashSet<>();
                bucket.add("br.com.archbase.plugin.manager.plugin.TestExtension");
                entries.put("plugin1", bucket);

                return entries;
            }

            @Override
            public Map<String, Set<String>> readClasspathStorages() {
                Map<String, Set<String>> entries = new LinkedHashMap<>();

                Set<String> bucket = new HashSet<>();
                bucket.add("br.com.archbase.plugin.manager.plugin.TestExtension");
                bucket.add("br.com.archbase.plugin.manager.plugin.FailTestExtension");
                entries.put(null, bucket);

                return entries;
            }

        };

        Set<String> result = instance.findClassNames(null);
        assertEquals(2, result.size());

        result = instance.findClassNames("plugin1");
        assertEquals(1, result.size());
    }

    @Test
    void findExtensionAnnotation() throws Exception {
        Compilation compilation = javac().compile(ExtensionAnnotationProcessorTest.Greeting,
                ExtensionAnnotationProcessorTest.WhazzupGreeting);
        assertThat(compilation).succeededWithoutWarnings();
        ImmutableList<JavaFileObject> generatedFiles = compilation.generatedFiles();
        assertEquals(2, generatedFiles.size());

        JavaFileObjectClassLoader classLoader = new JavaFileObjectClassLoader();
        Map<String, Class<?>> loadedClasses = classLoader.loadClasses(new ArrayList<>(generatedFiles));
        Class<?> clazz = loadedClasses.get("test.WhazzupGreeting");
        Extension extension = AbstractExtensionFinder.findExtensionAnnotation(clazz);
        assertNotNull(extension);
    }

    @Test
    void findExtensionAnnotationThatMissing() throws Exception {
        Compilation compilation = javac().compile(ExtensionAnnotationProcessorTest.Greeting,
                ExtensionAnnotationProcessorTest.SpinnakerExtension_NoExtension,
                ExtensionAnnotationProcessorTest.WhazzupGreeting_SpinnakerExtension);
        assertThat(compilation).succeededWithoutWarnings();
        ImmutableList<JavaFileObject> generatedFiles = compilation.generatedFiles();
        assertEquals(3, generatedFiles.size());

        JavaFileObjectClassLoader classLoader = new JavaFileObjectClassLoader();
        Map<String, Class<?>> loadedClasses = classLoader.loadClasses(new ArrayList<>(generatedFiles));
        Class<?> clazz = loadedClasses.get("test.WhazzupGreeting");
        Extension extension = AbstractExtensionFinder.findExtensionAnnotation(clazz);
        assertNull(extension);
    }

    static class JavaFileObjectClassLoader extends ClassLoader {

        private static String getClassName(JavaFileObject object) {
            String name = object.getName();
            // Remove "/CLASS_OUT/" from head and ".class" from tail
            name = name.substring(14, name.length() - 6);
            name = name.replace('/', '.');

            return name;
        }

        public Map<String, Class<?>> loadClasses(List<JavaFileObject> classes) throws IOException {
            // Sort generated ".class" by lastModified field
            classes.sort(Comparator.comparingLong(JavaFileObject::getLastModified));

            // Load classes
            Map<String, Class<?>> loadedClasses = new HashMap<>(classes.size());
            for (JavaFileObject clazz : classes) {
                String className = getClassName(clazz);
                byte[] data = ByteStreams.toByteArray(clazz.openInputStream());
                Class<?> loadedClass = defineClass(className, data, 0, data.length);
                loadedClasses.put(className, loadedClass);
            }

            return loadedClasses;
        }

    }

}
