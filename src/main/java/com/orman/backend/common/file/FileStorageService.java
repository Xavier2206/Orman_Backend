package com.orman.backend.common.file;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/** Provider-neutral persistence for private file objects. Keys are relative object names. */
public interface FileStorageService {

    void put(String key, InputStream content, long contentLength, String contentType);

    default void put(String key, byte[] content, String contentType) {
        put(key, new ByteArrayInputStream(content), content.length, contentType);
    }

    StoredObject get(String key);

    void delete(String key);

    boolean exists(String key);
}
