package br.com.archbase.plugin.manager.update;

import br.com.archbase.plugin.manager.update.verifier.BasicVerifier;
import br.com.archbase.plugin.manager.update.verifier.Sha512SumVerifier;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

public class FileVerifiersTest {
    @Test
    public void testSha512Verifier() throws IOException, VerifyException {
        FileVerifier fileVerifier = new Sha512SumVerifier();
        Path testFile = Files.createTempFile("test", ".tmp");
        Files.write(testFile, "Test".getBytes("utf-8"));
        String sha512sum = "c6ee9e33cf5c6715a1d148fd73f7318884b41adcb916021e2bc0e800a5c5dd97f5142178f6ae88c8fdd98e1afb0ce4c8d2c54b5f37b30b7da1997bb33b0b8a31";
        fileVerifier.verify(new FileVerifier.Context("foo", new Date(), "1.2.3",
                null, "http://example.com/repo/foo-1.2.3.zip", sha512sum), testFile);
        Files.delete(testFile);
    }

    @Test
    public void testBasicVerifier() throws IOException, VerifyException {
        FileVerifier fileVerifier = new BasicVerifier();
        Path testFile = Files.createTempFile("test", ".tmp");
        Files.write(testFile, "Test".getBytes("utf-8"));
        fileVerifier.verify(new FileVerifier.Context("foo", new Date(), "1.2.3",
                null, "http://example.com/repo/foo-1.2.3.zip", null), testFile);
        Files.delete(testFile);
    }

    @Test(expected = VerifyException.class)
    public void testBasicVerifierEmpty() throws IOException, VerifyException {
        FileVerifier fileVerifier = new BasicVerifier();
        Path testFile = Files.createTempFile("test", ".tmp");
        fileVerifier.verify(new FileVerifier.Context("foo", new Date(), "1.2.3",
                null, "http://example.com/repo/foo-1.2.3.zip", null), testFile);
        Files.delete(testFile);
    }

    @Test(expected = VerifyException.class)
    public void testBasicVerifierNotExists() throws IOException, VerifyException {
        FileVerifier fileVerifier = new BasicVerifier();
        Path testFile = Paths.get("/tmp/foo/bar");
        fileVerifier.verify(new FileVerifier.Context("foo", new Date(), "1.2.3",
                null, "http://example.com/repo/foo-1.2.3.zip", null), testFile);
        Files.delete(testFile);
    }
}