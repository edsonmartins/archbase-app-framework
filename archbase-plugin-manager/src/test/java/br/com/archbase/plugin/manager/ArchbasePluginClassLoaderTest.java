package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.plugin.PluginZip;
import br.com.archbase.plugin.manager.util.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


class ArchbasePluginClassLoaderTest {

    @TempDir
    Path pluginsPath;
    private TestArchbasePluginManager pluginManager;
    private TestArchbasePluginManager pluginManagerParentFirst;
    private DefaultArchbasePluginDescriptor pluginDependencyDescriptor;
    private DefaultArchbasePluginDescriptor pluginDescriptor;
    private PluginClassLoader parentLastPluginClassLoader;
    private PluginClassLoader parentFirstPluginClassLoader;
    private PluginClassLoader parentLastPluginDependencyClassLoader;
    private PluginClassLoader parentFirstPluginDependencyClassLoader;

    @BeforeAll
    static void setUpGlobal() throws IOException, URISyntaxException {
        Path parentClassPathBase = Paths.get(ArchbasePluginClassLoaderTest.class.getClassLoader().getResource(".").toURI());

        File metaInfFile = parentClassPathBase.resolve("META-INF").toFile();
        if (metaInfFile.mkdir()) {
            // Exclua o diretório apenas se este teste o tiver criado, protegendo para qualquer uso futuro do diretório.
            metaInfFile.deleteOnExit();
        }

        createFile(parentClassPathBase.resolve("META-INF").resolve("file-only-in-parent"));
        createFile(parentClassPathBase.resolve("META-INF").resolve("file-in-both-parent-and-dependency-and-plugin"));
        createFile(parentClassPathBase.resolve("META-INF").resolve("file-in-both-parent-and-dependency"));
        createFile(parentClassPathBase.resolve("META-INF").resolve("file-in-both-parent-and-plugin"));
    }

    private static void createFile(Path pathToFile) throws IOException {
        File file = pathToFile.toFile();

        file.deleteOnExit();
        assertTrue(file.exists() || file.createNewFile(), "falhou em criar '" + pathToFile + "'");
        try (PrintWriter printWriter = new PrintWriter(file)) {
            printWriter.write("parent");
        }
    }

    private static void assertFirstLine(String expected, URL resource) throws URISyntaxException, IOException {
        assertNotNull(resource);
        assertEquals(expected, Files.readAllLines(Paths.get(resource.toURI())).get(0));
    }

    private static void assertNumberOfResourcesAndFirstLineOfFirstElement(int expectedCount, String expectedFirstLine, Enumeration<URL> resources) throws URISyntaxException, IOException {
        List<URL> list = Collections.list(resources);
        assertEquals(expectedCount, list.size());

        URL firstResource = list.get(0);
        assertEquals(expectedFirstLine, Files.readAllLines(Paths.get(firstResource.toURI())).get(0));
    }

    @BeforeEach
    void setUp() throws IOException {
        pluginManager = new TestArchbasePluginManager(pluginsPath);
        pluginManagerParentFirst = new TestArchbasePluginManager(pluginsPath);

        pluginDependencyDescriptor = new DefaultArchbasePluginDescriptor();
        pluginDependencyDescriptor.setPluginId("myDependency");
        pluginDependencyDescriptor.setPluginVersion("1.2.3");
        pluginDependencyDescriptor.setPluginDescription("My plugin");
        pluginDependencyDescriptor.setDependencies("");
        pluginDependencyDescriptor.setProvider("Me");
        pluginDependencyDescriptor.setRequires("5.0.0");


        Path pluginDependencyPath = pluginsPath.resolve(pluginDependencyDescriptor.getPluginId() + "-" + pluginDependencyDescriptor.getVersion() + ".zip");
        PluginZip pluginDependencyZip = new PluginZip.Builder(pluginDependencyPath, pluginDependencyDescriptor.getPluginId())
                .pluginVersion(pluginDependencyDescriptor.getVersion())
                .addFile(Paths.get("classes/META-INF/dependency-file"), "dependency")
                .addFile(Paths.get("classes/META-INF/file-in-both-parent-and-dependency-and-plugin"), "dependency")
                .addFile(Paths.get("classes/META-INF/file-in-both-parent-and-dependency"), "dependency")
                .build();

        FileUtils.expandIfZip(pluginDependencyZip.path());

        PluginClasspath pluginDependencyClasspath = new DefaultArchbasePluginClasspath();

        parentLastPluginDependencyClassLoader = new PluginClassLoader(pluginManager, pluginDependencyDescriptor, ArchbasePluginClassLoaderTest.class.getClassLoader());
        parentFirstPluginDependencyClassLoader = new PluginClassLoader(pluginManagerParentFirst, pluginDependencyDescriptor, ArchbasePluginClassLoaderTest.class.getClassLoader(), true);

        pluginManager.addClassLoader(pluginDependencyDescriptor.getPluginId(), parentLastPluginDependencyClassLoader);
        pluginManagerParentFirst.addClassLoader(pluginDependencyDescriptor.getPluginId(), parentFirstPluginDependencyClassLoader);


        for (String classesDirectory : pluginDependencyClasspath.getClassesDirectories()) {
            File classesDirectoryFile = pluginDependencyZip.unzippedPath().resolve(classesDirectory).toFile();
            parentLastPluginDependencyClassLoader.addFile(classesDirectoryFile);
            parentFirstPluginDependencyClassLoader.addFile(classesDirectoryFile);
        }

        for (String jarsDirectory : pluginDependencyClasspath.getJarsDirectories()) {
            Path jarsDirectoryPath = pluginDependencyZip.unzippedPath().resolve(jarsDirectory);
            List<File> jars = FileUtils.getJars(jarsDirectoryPath);
            for (File jar : jars) {
                parentLastPluginDependencyClassLoader.addFile(jar);
                parentFirstPluginDependencyClassLoader.addFile(jar);
            }
        }

        pluginDescriptor = new DefaultArchbasePluginDescriptor();
        pluginDescriptor.setPluginId("myPlugin");
        pluginDescriptor.setPluginVersion("1.2.3");
        pluginDescriptor.setPluginDescription("My plugin");
        pluginDescriptor.setDependencies("myDependency");
        pluginDescriptor.setProvider("Me");
        pluginDescriptor.setRequires("5.0.0");

        Path pluginPath = pluginsPath.resolve(pluginDescriptor.getPluginId() + "-" + pluginDescriptor.getVersion() + ".zip");
        PluginZip pluginZip = new PluginZip.Builder(pluginPath, pluginDescriptor.getPluginId())
                .pluginVersion(pluginDescriptor.getVersion())
                .addFile(Paths.get("classes/META-INF/plugin-file"), "plugin")
                .addFile(Paths.get("classes/META-INF/file-in-both-parent-and-dependency-and-plugin"), "plugin")
                .addFile(Paths.get("classes/META-INF/file-in-both-parent-and-plugin"), "plugin")
                .build();

        FileUtils.expandIfZip(pluginZip.path());

        PluginClasspath pluginClasspath = new DefaultArchbasePluginClasspath();

        parentLastPluginClassLoader = new PluginClassLoader(pluginManager, pluginDescriptor, ArchbasePluginClassLoaderTest.class.getClassLoader());
        parentFirstPluginClassLoader = new PluginClassLoader(pluginManager, pluginDescriptor, ArchbasePluginClassLoaderTest.class.getClassLoader(), true);

        pluginManager.addClassLoader(pluginDescriptor.getPluginId(), parentLastPluginClassLoader);
        pluginManagerParentFirst.addClassLoader(pluginDescriptor.getPluginId(), parentFirstPluginClassLoader);

        for (String classesDirectory : pluginClasspath.getClassesDirectories()) {
            File classesDirectoryFile = pluginZip.unzippedPath().resolve(classesDirectory).toFile();
            parentLastPluginClassLoader.addFile(classesDirectoryFile);
            parentFirstPluginClassLoader.addFile(classesDirectoryFile);
        }

        for (String jarsDirectory : pluginClasspath.getJarsDirectories()) {
            Path jarsDirectoryPath = pluginZip.unzippedPath().resolve(jarsDirectory);
            List<File> jars = FileUtils.getJars(jarsDirectoryPath);
            for (File jar : jars) {
                parentLastPluginClassLoader.addFile(jar);
                parentFirstPluginClassLoader.addFile(jar);
            }
        }
    }

    @AfterEach
    void tearDown() {
        pluginManager = null;
        pluginDependencyDescriptor = null;
        pluginDescriptor = null;
        parentLastPluginClassLoader = null;
        parentFirstPluginClassLoader = null;
    }

    @Test
    void parentLastGetResourceNonExisting() {
        assertNull(parentLastPluginClassLoader.getResource("META-INF/non-existing-file"));
    }

    @Test
    void parentFirstGetResourceNonExisting() {
        assertNull(parentFirstPluginClassLoader.getResource("META-INF/non-existing-file"));
    }

    @Test
    void parentLastGetResourceExistsInParent() throws IOException, URISyntaxException {
        URL resource = parentLastPluginClassLoader.getResource("META-INF/file-only-in-parent");
        assertFirstLine("parent", resource);
    }

    @Test
    void parentFirstGetResourceExistsInParent() throws IOException, URISyntaxException {
        URL resource = parentFirstPluginClassLoader.getResource("META-INF/file-only-in-parent");
        assertFirstLine("parent", resource);
    }

    @Test
    void parentLastGetResourceExistsOnlyInPlugin() throws IOException, URISyntaxException {
        URL resource = parentLastPluginClassLoader.getResource("META-INF/plugin-file");
        assertFirstLine("plugin", resource);
    }

    @Test
    void parentFirstGetResourceExistsOnlyInPlugin() throws IOException, URISyntaxException {
        URL resource = parentFirstPluginClassLoader.getResource("META-INF/plugin-file");
        assertFirstLine("plugin", resource);
    }

    @Test
    void parentLastGetResourceExistsOnlyInDependnecy() throws IOException, URISyntaxException {
        URL resource = parentLastPluginClassLoader.getResource("META-INF/dependency-file");
        assertFirstLine("dependency", resource);
    }

    @Test
    void parentFirstGetResourceExistsOnlyInDependency() throws IOException, URISyntaxException {
        URL resource = parentFirstPluginClassLoader.getResource("META-INF/dependency-file");
        assertFirstLine("dependency", resource);
    }

    @Test
    void parentLastGetResourceExistsInBothParentAndPlugin() throws URISyntaxException, IOException {
        URL resource = parentLastPluginClassLoader.getResource("META-INF/file-in-both-parent-and-plugin");
        assertFirstLine("plugin", resource);
    }

    @Test
    void parentFirstGetResourceExistsInBothParentAndPlugin() throws URISyntaxException, IOException {
        URL resource = parentFirstPluginClassLoader.getResource("META-INF/file-in-both-parent-and-plugin");
        assertFirstLine("parent", resource);
    }

    @Test
    void parentLastGetResourceExistsInParentAndDependencyAndPlugin() throws URISyntaxException, IOException {
        URL resource = parentLastPluginClassLoader.getResource("META-INF/file-in-both-parent-and-dependency-and-plugin");
        assertFirstLine("plugin", resource);
    }

    @Test
    void parentFirstGetResourceExistsInParentAndDependencyAndPlugin() throws URISyntaxException, IOException {
        URL resource = parentFirstPluginClassLoader.getResource("META-INF/file-in-both-parent-and-dependency-and-plugin");
        assertFirstLine("parent", resource);
    }

    @Test
    void parentLastGetResourcesNonExisting() throws IOException {
        assertFalse(parentLastPluginClassLoader.getResources("META-INF/non-existing-file").hasMoreElements());
    }

    @Test
    void parentFirstGetResourcesNonExisting() throws IOException {
        assertFalse(parentFirstPluginClassLoader.getResources("META-INF/non-existing-file").hasMoreElements());
    }

    @Test
    void parentLastGetResourcesExistsInParent() throws IOException, URISyntaxException {
        Enumeration<URL> resources = parentLastPluginClassLoader.getResources("META-INF/file-only-in-parent");
        assertNumberOfResourcesAndFirstLineOfFirstElement(1, "parent", resources);
    }

    @Test
    void parentFirstGetResourcesExistsInParent() throws IOException, URISyntaxException {
        Enumeration<URL> resources = parentFirstPluginClassLoader.getResources("META-INF/file-only-in-parent");
        assertNumberOfResourcesAndFirstLineOfFirstElement(1, "parent", resources);
    }

    @Test
    void parentLastGetResourcesExistsOnlyInDependency() throws IOException, URISyntaxException {
        Enumeration<URL> resources = parentLastPluginClassLoader.getResources("META-INF/dependency-file");
        assertNumberOfResourcesAndFirstLineOfFirstElement(1, "dependency", resources);
    }

    @Test
    void parentFirstGetResourcesExistsOnlyInDependency() throws IOException, URISyntaxException {
        Enumeration<URL> resources = parentFirstPluginClassLoader.getResources("META-INF/dependency-file");
        assertNumberOfResourcesAndFirstLineOfFirstElement(1, "dependency", resources);
    }

    @Test
    void parentLastGetResourcesExistsOnlyInPlugin() throws IOException, URISyntaxException {
        Enumeration<URL> resources = parentLastPluginClassLoader.getResources("META-INF/plugin-file");
        assertNumberOfResourcesAndFirstLineOfFirstElement(1, "plugin", resources);
    }

    @Test
    void parentFirstGetResourcesExistsOnlyInPlugin() throws IOException, URISyntaxException {
        Enumeration<URL> resources = parentFirstPluginClassLoader.getResources("META-INF/plugin-file");
        assertNumberOfResourcesAndFirstLineOfFirstElement(1, "plugin", resources);
    }

    @Test
    void parentLastGetResourcesExistsInBothParentAndPlugin() throws URISyntaxException, IOException {
        Enumeration<URL> resources = parentLastPluginClassLoader.getResources("META-INF/file-in-both-parent-and-plugin");
        assertNumberOfResourcesAndFirstLineOfFirstElement(2, "plugin", resources);
    }

    @Test
    void parentFirstGetResourcesExistsInBothParentAndPlugin() throws URISyntaxException, IOException {
        Enumeration<URL> resources = parentFirstPluginClassLoader.getResources("META-INF/file-in-both-parent-and-plugin");
        assertNumberOfResourcesAndFirstLineOfFirstElement(2, "parent", resources);
    }

    @Test
    void parentLastGetResourcesExistsInParentAndDependencyAndPlugin() throws URISyntaxException, IOException {
        Enumeration<URL> resources = parentLastPluginClassLoader.getResources("META-INF/file-in-both-parent-and-dependency-and-plugin");
        assertNumberOfResourcesAndFirstLineOfFirstElement(3, "plugin", resources);
    }

    @Test
    void parentFirstGetResourcesExistsInParentAndDependencyAndPlugin() throws URISyntaxException, IOException {
        Enumeration<URL> resources = parentFirstPluginClassLoader.getResources("META-INF/file-in-both-parent-and-dependency-and-plugin");
        assertNumberOfResourcesAndFirstLineOfFirstElement(3, "parent", resources);
    }

    class TestArchbasePluginManager extends DefaultArchbasePluginManager {

        public TestArchbasePluginManager(Path pluginsPath) {
            super(pluginsPath);
        }

        void addClassLoader(String pluginId, PluginClassLoader classLoader) {
            getPluginClassLoaders().put(pluginId, classLoader);
        }
    }
}