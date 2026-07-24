package io.github.onedream921.alphavue.modules.file;

import java.io.IOException;
import java.io.InputStream;

/** Boundary for object storage implementations used by file metadata operations. */
public interface StorageProvider {

    void store(String key, InputStream input, String contentType) throws IOException;

    void delete(String key) throws IOException;

    String publicUrl(String key);
}
