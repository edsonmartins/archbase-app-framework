package br.com.archbase.plugin.manager.update;

import br.com.archbase.plugin.manager.*;
import br.com.archbase.plugin.manager.update.util.NopPlugin;
import br.com.archbase.plugin.manager.update.util.PropertiesArchbasePluginManager;
import com.google.gson.GsonBuilder;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Downloads de teste etc.
 */
public class InstallAndDownloadTest {

    private static final Logger log = LoggerFactory.getLogger(InstallAndDownloadTest.class);

    private Path downloadRepoDir;
    private Path pluginFolderDir;

    private MockZipPlugin p1;
    private MockZipPlugin p2;
    private MockZipPlugin p3;
    private MockZipPlugin p4;
    private MockZipPlugin p5;

    private ArchbasePluginManager archbasePluginManager;
    private UpdateManager updateManager;
    private VersionManager versionManager;

    private String systemVersion;
    private URL repoUrl;
    private List<PluginWrapper> installed;

    @Before
    public void setup() throws IOException {
        downloadRepoDir = Files.createTempDirectory("archbase-repo");
        downloadRepoDir.toFile().deleteOnExit();

        pluginFolderDir = Files.createTempDirectory("archbase-plugins");

        p1 = new MockZipPlugin("myPlugin", "1.2.3", "my-plugin-1.2.3", "my-plugin-1.2.3.zip", "Mar 22, 2017 9:00:35 PM");
        p2 = new MockZipPlugin("myPlugin", "2.0.0", "my-plugin-2.0.0", "my-plugin-2.0.0.ZIP");
        p3 = new MockZipPlugin("other", "3.0.0", "other-3.0.0", "other-3.0.0.Zip");
        p4 = new MockZipPlugin("other", "3.0.1", "other-3.0.1", "other-3.0.1.Zip", "2017-01-31T12:34:56Z");
        p5 = new MockZipPlugin("wrongDate", "4.0.1", "wrong-4.0.1", "wrong-4.0.1.Zip", "wrong");

        archbasePluginManager = new PropertiesArchbasePluginManager(pluginFolderDir);
        systemVersion = "1.8.0";
        archbasePluginManager.setSystemVersion(systemVersion); // Only p2 and p3 are valid

        versionManager = archbasePluginManager.getVersionManager();

        repoUrl = new URL("file:" + downloadRepoDir.toAbsolutePath().toString() + "/");
        UpdateRepository local = new DefaultUpdateRepository("local", repoUrl);
        p1.create();
        p2.create();
        p3.create();
        p5.create();

        Path pluginsJson = downloadRepoDir.resolve("plugins.json");
        BufferedWriter writer = Files.newBufferedWriter(pluginsJson, Charset.defaultCharset(), StandardOpenOption.CREATE_NEW);
        String jsonForPlugins = getJsonForPlugins(p1, p2, p3, p4, p5);
        writer.write(jsonForPlugins);
        writer.close();

        updateManager = new UpdateManager(archbasePluginManager, Collections.singletonList(local));
    }

    @Test
    public void findRightVersions() {
        assertEquals(1, updateManager.repositories.size());
        assertEquals(3, updateManager.getPlugins().size());
        assertEquals("2.0.0", updateManager.getLastPluginRelease("myPlugin").version);
        assertEquals("3.0.1", updateManager.getLastPluginRelease("other").version);
    }

    @Test
    public void tolerantDateParsing() throws Exception {
        assertEquals(dateFor("2016-12-31"), updateManager.getLastPluginRelease("myPlugin").date);
        assertTrue(updateManager.getLastPluginRelease("other").date.after(dateFor("2017-01-31")));
        assertTrue(updateManager.getLastPluginRelease("other").date.before(dateFor("2017-02-01")));
        assertEquals(dateFor("1970-01-01"), updateManager.getLastPluginRelease("wrongDate").date);
    }

    @Test
    public void install() {
        assertFalse(Files.exists(pluginFolderDir.resolve(p3.zipname)));
        assertTrue(updateManager.installPlugin("other", "3.0.0"));
        assertTrue(Files.exists(pluginFolderDir.resolve(p3.zipname)));
    }

    @Test
    public void installOldVersion() {
        assertTrue(updateManager.installPlugin("myPlugin", "1.2.3"));
    }

    @Test
    public void update() {
        assertTrue(updateManager.installPlugin("myPlugin", "1.2.3"));
        assertTrue(updateManager.hasUpdates());
        assertEquals(1, updateManager.getUpdates().size());
        assertTrue(updateManager.updatePlugin("myPlugin", null)); // latest release
        assertTrue(Files.exists(pluginFolderDir.resolve(p2.zipname)));
        assertTrue(Files.exists(pluginFolderDir.resolve(p2.pluginRepoUnzippedFolder)));
        assertFalse(Files.exists(pluginFolderDir.resolve(p1.zipname)));
        assertFalse(Files.exists(pluginFolderDir.resolve(p1.pluginRepoUnzippedFolder)));
    }

    @Test(expected = PluginRuntimeException.class)
    public void updateVersionNotExist() {
        validateInstallPlugin("myPlugin", "1.2.3");
        updateManager.updatePlugin("myPlugin", "9.9.9");
    }

    private void validateInstallPlugin(String myPlugin, String s) {
        assertTrue(updateManager.installPlugin(myPlugin, s));
    }

    @Test
    public void noUpdateAvailable() {
        assertTrue(updateManager.installPlugin("myPlugin", null)); // Instale o mais recente
        assertFalse(updateManager.updatePlugin("myPlugin", null)); // Atualize para o mais recente
    }

    @Test(expected = IllegalArgumentException.class)
    public void uninstallNonExisting() {
        updateManager.uninstallPlugin("other");
    }

    @Test
    public void uninstall() {
        updateManager.installPlugin("other", "3.0.0");
        assertTrue(updateManager.uninstallPlugin("other"));
    }

    @Test
    public void repositoryIdIsFilled() {
        for (PluginInfo info : updateManager.getAvailablePlugins()) {
            assertEquals("local", info.getRepositoryId());
        }
    }


    private Date dateFor(String date) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        return sdf.parse(date);
    }

    private String getJsonForPlugins(MockZipPlugin... plugins) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        Map<String, List<MockZipPlugin>> pluginMap = new HashMap<>();
        for (MockZipPlugin p : plugins) {
            if (pluginMap.containsKey(p.id)) {
                List<MockZipPlugin> l = pluginMap.get(p.id);
                l.add(p);
            } else {
                List<MockZipPlugin> l = new ArrayList<>();
                l.add(p);
                pluginMap.put(p.id, l);
            }
        }

        List list = new ArrayList<>();
        for (List<MockZipPlugin> l : pluginMap.values()) {
            Map<String, Object> info = new HashMap<>();
            info.put("id", l.get(0).id);
            List<Object> releases = new ArrayList<>();
            for (MockZipPlugin p : l) {
                Map<String, String> releaseInfo = new HashMap<>();
                releaseInfo.put("version", p.version);
                releaseInfo.put("url", p.zipname);
                releaseInfo.put("date", p.dateStr);
                releaseInfo.put("sha512sum", p.getSha512());
                releases.add(releaseInfo);
            }
            info.put("releases", releases);
            list.add(info);
        }

        return gsonBuilder.create().toJson(list);
    }

    private class MockZipPlugin {

        public final String id;
        public final String version;
        public final String filenameUnzipped;
        public final Path updateRepoZipFile;
        public final Path pluginRepoUnzippedFolder;
        private final Path propsFile;
        private final URI fileURI;
        public String zipname;
        public TestArchbasePluginDescriptor descriptor;
        public String dateStr;

        public MockZipPlugin(String id, String version, String filename, String zipname, String dateStr) {
            this.id = id;
            this.version = version;
            this.filenameUnzipped = filename;
            this.zipname = zipname;
            this.dateStr = dateStr;

            updateRepoZipFile = downloadRepoDir.resolve(zipname).toAbsolutePath();
            pluginRepoUnzippedFolder = pluginFolderDir.resolve(filename);
            propsFile = downloadRepoDir.resolve("my.properties");
            fileURI = URI.create("jar:file:" + updateRepoZipFile.toString());

            descriptor = new TestArchbasePluginDescriptor();
            descriptor.setPluginId(id);
            descriptor.setPluginVersion(version);
        }

        public MockZipPlugin(String id, String version, String filename, String zipname) {
            this(id, version, filename, zipname, "2016-12-31");
        }

        public void create() throws IOException {
            try (FileSystem zipfs = FileSystems.newFileSystem(fileURI, Collections.singletonMap("create", "true"))) {
                Path propsInZip = zipfs.getPath("/" + propsFile.getFileName().toString());
                BufferedWriter br = new BufferedWriter(new FileWriter(propsFile.toString()));
                br.write("plugin.id=" + id);
                br.newLine();
                br.write("plugin.version=" + version);
                br.newLine();
                br.write("plugin.class=" + NopPlugin.class.getName());
                br.close();
                Files.move(propsFile, propsInZip);
            }
        }

        public String getSha512() {
            try {
                String checksum = DigestUtils.sha512Hex(Files.newInputStream(updateRepoZipFile));
                log.debug("Soma SHA gerada para o arquivo: {} ", checksum);
                return checksum;
            } catch (IOException e) {
                return null;
            }
        }
    }

}
