package com.orman.backend.common.file;

import java.io.InputStream;
import org.springframework.core.io.InputStreamResource;

/** Spring HTTP adapter that retains the provider's known length without buffering its stream. */
public final class StorageInputStreamResource extends InputStreamResource {

    private final long contentLength;

    public StorageInputStreamResource(StoredObject object) {
        super(object.content());
        this.contentLength = object.contentLength();
    }

    @Override
    public long contentLength() {
        return contentLength;
    }
}
