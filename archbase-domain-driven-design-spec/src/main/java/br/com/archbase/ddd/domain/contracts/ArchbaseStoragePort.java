package br.com.archbase.ddd.domain.contracts;

import java.io.InputStream;

public interface ArchbaseStoragePort {

    String uploadFile(String objectName, InputStream data, String contentType);

    InputStream downloadFile(String objectName);

    void deleteFile(String objectName);

    String getStorageUrl();

    String getStoragePath();
}
