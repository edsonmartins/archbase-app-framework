package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.plugin.FailTestExtension;
import br.com.archbase.plugin.manager.plugin.TestExtension;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;


class SingletonExtensionFactoryTest {

    @Test
    void create() {
        ExtensionFactory extensionFactory = new SingletonArchbaseExtensionFactory();
        Object extensionOne = extensionFactory.create(TestExtension.class);
        Object extensionTwo = extensionFactory.create(TestExtension.class);
        assertSame(extensionOne, extensionTwo);
    }

    @Test
    void createNewEachTime() {
        ExtensionFactory extensionFactory = new SingletonArchbaseExtensionFactory(FailTestExtension.class.getName());
        Object extensionOne = extensionFactory.create(TestExtension.class);
        Object extensionTwo = extensionFactory.create(TestExtension.class);
        assertNotSame(extensionOne, extensionTwo);
    }

    @Test
    @SuppressWarnings("unchecked")
    void createNewEachTimeFromDifferentClassLoaders() throws Exception {
        ExtensionFactory extensionFactory = new SingletonArchbaseExtensionFactory();

        // Obter locais de caminho de classe
        URL[] classpathReferences = getClasspathReferences();

        // Crie carregadores de classe diferentes para as referências de caminho de classe e classes de carga, respectivamente
        ClassLoader klassLoaderOne = new URLClassLoader(classpathReferences, null);
        Class klassOne = klassLoaderOne.loadClass(TestExtension.class.getName());
        ClassLoader klassLoaderTwo = new URLClassLoader(classpathReferences, null);
        Class klassTwo = klassLoaderTwo.loadClass(TestExtension.class.getName());

        // create instances
        Object instanceOne = extensionFactory.create(klassOne);
        Object instanceTwo = extensionFactory.create(klassTwo);

        // afirma que as instâncias não são iguais
        assertNotSame(instanceOne, instanceTwo);
    }

    private URL[] getClasspathReferences() throws MalformedURLException {
        String classpathProperty = System.getProperty("java.class.path");

        String[] classpaths = classpathProperty.split(":");
        URL[] uris = new URL[classpaths.length];

        for (int index = 0; index < classpaths.length; index++) {
            uris[index] = new File(classpaths[index]).toURI().toURL();
        }
        return uris;
    }

}
