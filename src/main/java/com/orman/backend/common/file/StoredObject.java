package com.orman.backend.common.file;

import java.io.InputStream;
import java.util.Objects;

/** A one-shot stream and its known size, suitable for HTTP streaming. */
public record StoredObject(InputStream content, long contentLength) {

    public StoredObject {
        Objects.requireNonNull(content, "content");
        if (contentLength < 0) {
            throw new IllegalArgumentException("contentLength must not be negative");
        }
    }
}
