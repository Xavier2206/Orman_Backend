package com.orman.backend.common.file;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageServiceTest {

    @TempDir
    Path root;

    @Test
    void storesReadsAndDeletesAnObject() throws Exception {
        FileStorageService storage = new LocalFileStorageService(Map.of("personas", root.toString()));
        byte[] bytes = "photo-bytes".getBytes(StandardCharsets.UTF_8);

        storage.put("personas/14/photo.jpg", new ByteArrayInputStream(bytes), bytes.length, "image/jpeg");

        assertThat(storage.exists("personas/14/photo.jpg")).isTrue();
        StoredObject object = storage.get("personas/14/photo.jpg");
        assertThat(object.contentLength()).isEqualTo(bytes.length);
        assertThat(object.content().readAllBytes()).containsExactly(bytes);
        storage.delete("personas/14/photo.jpg");
        assertThat(storage.exists("personas/14/photo.jpg")).isFalse();
    }

    @Test
    void reportsMissingObjectsAndRejectsPathTraversal() {
        FileStorageService storage = new LocalFileStorageService(Map.of("personas", root.toString()));

        assertThatThrownBy(() -> storage.get("personas/14/missing.jpg"))
                .isInstanceOf(StorageObjectNotFoundException.class);
        assertThatThrownBy(() -> storage.put("personas/../../outside.txt",
                new ByteArrayInputStream(new byte[0]), 0, "text/plain"))
                .isInstanceOf(StorageException.class);
        assertThatThrownBy(() -> storage.delete("personas\\14\\photo.jpg"))
                .isInstanceOf(StorageException.class);
    }
}
