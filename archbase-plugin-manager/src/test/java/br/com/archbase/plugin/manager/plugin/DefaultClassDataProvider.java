package br.com.archbase.plugin.manager.plugin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Obtenha dados da classe do caminho da classe.
 */
class DefaultClassDataProvider implements ClassDataProvider {

    @Override
    public byte[] getClassData(String className) {
        String path = className.replace('.', '/') + ".class";
        InputStream classDataStream = getClass().getClassLoader().getResourceAsStream(path);
        if (classDataStream == null) {
            throw new RuntimeException("Cannot find class data");
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            copyStream(classDataStream, outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];

        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
    }

}
