package com.orman.backend.common.file;

import java.util.Arrays;

final class StorageKeys {

    private StorageKeys() {
    }

    static String validate(String key) {
        if (key == null || key.isBlank() || key.startsWith("/") || key.indexOf('/') <= 0 || key.indexOf('\\') >= 0
                || key.indexOf('\0') >= 0) {
            throw new StorageException("La clave de almacenamiento no es válida.");
        }
        if (Arrays.stream(key.split("/", -1)).anyMatch(part -> part.isBlank() || part.equals(".") || part.equals(".."))) {
            throw new StorageException("La clave de almacenamiento no es válida.");
        }
        return key;
    }
}
