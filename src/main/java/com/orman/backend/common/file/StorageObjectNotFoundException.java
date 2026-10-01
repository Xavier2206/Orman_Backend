package com.orman.backend.common.file;

public class StorageObjectNotFoundException extends StorageException {

    public StorageObjectNotFoundException() {
        super("El objeto solicitado no está disponible.");
    }
}
