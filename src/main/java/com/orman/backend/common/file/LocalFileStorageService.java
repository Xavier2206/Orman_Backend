package com.orman.backend.common.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

/** Local provider. Each top-level key namespace is mapped to its existing configured root. */
public final class LocalFileStorageService implements FileStorageService {

    private final Map<String, Path> roots;

    public LocalFileStorageService(Map<String, String> configuredRoots) {
        Map<String, Path> normalized = new HashMap<>();
        configuredRoots.forEach((namespace, root) -> normalized.put(namespace,
                Path.of(root).toAbsolutePath().normalize()));
        this.roots = Map.copyOf(normalized);
    }

    @Override
    public void put(String key, InputStream content, long contentLength, String contentType) {
        Path target = resolve(key);
        Path temporary = null;
        try {
            Path realRoot = ensureRoot(target.getParent(), key);
            assertContainedParent(target.getParent(), realRoot);
            if (Files.isSymbolicLink(target)) {
                throw new StorageException("La clave de almacenamiento no es válida.");
            }
            temporary = Files.createTempFile(target.getParent(), ".orman-storage-", ".tmp");
            long copied = 0;
            byte[] buffer = new byte[8192];
            try (InputStream input = content; var output = Files.newOutputStream(temporary,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                int count;
                while ((count = input.read(buffer)) != -1) {
                    copied += count;
                    if (copied > contentLength) {
                        throw new StorageException("El tamaño del objeto no coincide con el contenido.");
                    }
                    output.write(buffer, 0, count);
                }
            }
            if (copied != contentLength) {
                throw new StorageException("El tamaño del objeto no coincide con el contenido.");
            }
            assertContainedParent(target.getParent(), realRoot);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            temporary = null;
        } catch (StorageException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new StorageException("No fue posible almacenar el objeto.", exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // The failed operation is reported above; temporary cleanup is best effort.
                }
            }
        }
    }

    @Override
    public StoredObject get(String key) {
        Path file = resolve(key);
        try {
            Path realRoot = roots.get(namespace(key)).toRealPath();
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(file)) {
                throw new StorageObjectNotFoundException();
            }
            Path realFile = file.toRealPath(LinkOption.NOFOLLOW_LINKS);
            if (!realFile.startsWith(realRoot)) {
                throw new StorageException("La clave de almacenamiento no es válida.");
            }
            return new StoredObject(Files.newInputStream(realFile, LinkOption.NOFOLLOW_LINKS),
                    Files.size(realFile));
        } catch (StorageException exception) {
            throw exception;
        } catch (IOException exception) {
            if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
                throw new StorageObjectNotFoundException();
            }
            throw new StorageException("No fue posible leer el objeto.", exception);
        }
    }

    @Override
    public void delete(String key) {
        Path file = resolve(key);
        try {
            Path realRoot = ensureRoot(file.getParent(), key);
            assertContainedParent(file.getParent(), realRoot);
            if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
                if (Files.isSymbolicLink(file)) {
                    throw new StorageException("La clave de almacenamiento no es válida.");
                }
                Files.delete(file);
            }
        } catch (StorageException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new StorageException("No fue posible eliminar el objeto.", exception);
        }
    }

    @Override
    public boolean exists(String key) {
        Path file = resolve(key);
        if (Files.isSymbolicLink(file) || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }
        try {
            Path root = roots.get(namespace(key)).toRealPath();
            return file.toRealPath(LinkOption.NOFOLLOW_LINKS).startsWith(root);
        } catch (IOException exception) {
            return false;
        }
    }

    private Path resolve(String rawKey) {
        String key = StorageKeys.validate(rawKey);
        String namespace = namespace(key);
        Path root = roots.get(namespace);
        if (root == null) {
            throw new StorageException("El espacio de almacenamiento no está configurado.");
        }
        Path relative = Path.of(key.replace('/', java.io.File.separatorChar)).normalize();
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            throw new StorageException("La clave de almacenamiento no es válida.");
        }
        return target;
    }

    private String namespace(String key) {
        return key.substring(0, key.indexOf('/'));
    }

    private Path ensureRoot(Path parent, String key) throws IOException {
        Path root = roots.get(namespace(key));
        Files.createDirectories(root);
        Path realRoot = root.toRealPath();
        Files.createDirectories(parent);
        return realRoot;
    }

    private void assertContainedParent(Path parent, Path realRoot) throws IOException {
        Path realParent = parent.toRealPath();
        if (!realParent.startsWith(realRoot)) {
            throw new StorageException("La clave de almacenamiento no es válida.");
        }
    }
}
