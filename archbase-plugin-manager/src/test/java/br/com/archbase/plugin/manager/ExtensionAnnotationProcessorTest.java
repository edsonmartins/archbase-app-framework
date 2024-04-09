package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.processor.ExtensionAnnotationProcessor;
import br.com.archbase.plugin.manager.processor.LegacyExtensionStorage;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import java.util.*;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.jupiter.api.Assertions.assertEquals;


@SuppressWarnings("all")
class ExtensionAnnotationProcessorTest {

    static final JavaFileObject Greeting = JavaFileObjects.forSourceLines(
            "Greeting",
            "package test;",
            "import br.com.archbase.plugin.manager.ExtensionPoint;",
            "",
            "public interface Greeting extends ExtensionPoint {",
            "   String getGreeting();",
            "}");

    static final JavaFileObject WhazzupGreeting = JavaFileObjects.forSourceLines(
            "WhazzupGreeting",
            "package test;",
            "import br.com.archbase.plugin.manager.Extension;",
            "",
            "@Extension",
            "public class WhazzupGreeting implements Greeting {",
            "   @Override",
            "    public String getGreeting() {",
            "       return \"Whazzup\";",
            "    }",
            "}");

    static final JavaFileObject WhazzupGreeting_NoExtensionPoint = JavaFileObjects.forSourceLines(
            "WhazzupGreeting",
            "package test;",
            "import br.com.archbase.plugin.manager.Extension;",
            "",
            "@Extension",
            "public class WhazzupGreeting {",
            "   @Override",
            "    public String getGreeting() {",
            "       return \"Whazzup\";",
            "    }",
            "}");

    static final JavaFileObject SpinnakerExtension = JavaFileObjects.forSourceLines(
            "SpinnakerExtension",
            "package test;",
            "",
            "import br.com.archbase.plugin.manager.Extension;",
            "import java.lang.annotation.Documented;",
            "import java.lang.annotation.ElementType;",
            "import java.lang.annotation.Retention;",
            "import java.lang.annotation.RetentionPolicy;",
            "import java.lang.annotation.Target;",
            "",
            "@Extension",
            "@Retention(RetentionPolicy.RUNTIME)",
            "@Target(ElementType.TYPE)",
            "@Documented",
            "public @interface SpinnakerExtension {",
            "}");

    static final JavaFileObject WhazzupGreeting_SpinnakerExtension = JavaFileObjects.forSourceLines(
            "WhazzupGreeting",
            "package test;",
            "",
            "@SpinnakerExtension",
            "public class WhazzupGreeting implements Greeting {",
            "   @Override",
            "    public String getGreeting() {",
            "       return \"Whazzup\";",
            "    }",
            "}");

    /**
     * The same like {@link #SpinnakerExtension} but without {@code Extension} annotation.
     */
    static final JavaFileObject SpinnakerExtension_NoExtension = JavaFileObjects.forSourceLines(
            "SpinnakerExtension",
            "package test;",
            "",
            "import br.com.archbase.plugin.manager.Extension;",
            "import java.lang.annotation.Documented;",
            "import java.lang.annotation.ElementType;",
            "import java.lang.annotation.Retention;",
            "import java.lang.annotation.RetentionPolicy;",
            "import java.lang.annotation.Target;",
            "",
//        "@Extension",
            "@Retention(RetentionPolicy.RUNTIME)",
            "@Target(ElementType.TYPE)",
            "@Documented",
            "public @interface SpinnakerExtension {",
            "}");

    @Test
    public void getSupportedAnnotationTypes() {
        ExtensionAnnotationProcessor instance = new ExtensionAnnotationProcessor();
        Set<String> result = instance.getSupportedAnnotationTypes();
        assertEquals(1, result.size());
        assertEquals("*", result.iterator().next());
    }

    @Test
    public void getSupportedOptions() {
        ExtensionAnnotationProcessor instance = new ExtensionAnnotationProcessor();
        Set<String> result = instance.getSupportedOptions();
        assertEquals(2, result.size());
    }

    @Test
    public void options() {
        ExtensionAnnotationProcessor processor = new ExtensionAnnotationProcessor();
        Compilation compilation = javac().withProcessors(processor).withOptions("-Ab=2", "-Ac=3")
                .compile(Greeting, WhazzupGreeting);
        assertEquals(compilation.status(), Compilation.Status.SUCCESS);
        Map<String, String> options = new HashMap<>();
        options.put("b", "2");
        options.put("c", "3");
        assertEquals(options, processor.getProcessingEnvironment().getOptions());
    }

    @Test
    public void storage() {
        ExtensionAnnotationProcessor processor = new ExtensionAnnotationProcessor();
        Compilation compilation = javac().withProcessors(processor).compile(Greeting, WhazzupGreeting);
        assertEquals(compilation.status(), Compilation.Status.SUCCESS);
        assertEquals(processor.getStorage().getClass(), LegacyExtensionStorage.class);
    }

    @Test
    public void compileWithoutError() {
        ExtensionAnnotationProcessor processor = new ExtensionAnnotationProcessor();
        Compilation compilation = javac().withProcessors(processor).compile(Greeting, WhazzupGreeting);
        assertThat(compilation).succeededWithoutWarnings();
    }

    @Test
    public void compileWithError() {
        ExtensionAnnotationProcessor processor = new ExtensionAnnotationProcessor();
        Compilation compilation = javac().withProcessors(processor).compile(Greeting, WhazzupGreeting_NoExtensionPoint);
        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("não implementa ExtensionPoint")
                .inFile(WhazzupGreeting_NoExtensionPoint)
                .onLine(5)
                .atColumn(8);
    }

    @Test
    public void getExtensions() {
        ExtensionAnnotationProcessor processor = new ExtensionAnnotationProcessor();
        Compilation compilation = javac().withProcessors(processor).compile(Greeting, WhazzupGreeting);
        assertThat(compilation).succeededWithoutWarnings();
        Map<String, Set<String>> extensions = new HashMap<>();
        extensions.put("test.Greeting", new HashSet<>(Collections.singletonList("test.WhazzupGreeting")));
        assertEquals(extensions, processor.getExtensions());
    }

    @Test
    public void compileNestedExtensionAnnotation() {
        ExtensionAnnotationProcessor processor = new ExtensionAnnotationProcessor();
        Compilation compilation = javac().withProcessors(processor).compile(Greeting, SpinnakerExtension, WhazzupGreeting_SpinnakerExtension);
        assertThat(compilation).succeededWithoutWarnings();
        Map<String, Set<String>> extensions = new HashMap<>();
        extensions.put("test.Greeting", new HashSet<>(Collections.singletonList("test.WhazzupGreeting")));
        assertEquals(extensions, processor.getExtensions());
    }

}
