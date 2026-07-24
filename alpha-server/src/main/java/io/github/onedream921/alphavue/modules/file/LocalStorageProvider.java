package io.github.onedream921.alphavue.modules.file;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Stores objects below one configured root and rejects paths that escape it. */
@Component
public class LocalStorageProvider implements StorageProvider {

    public static final String NAME = "local";

    private final Path root;
    private final String publicUrlBase;

    public LocalStorageProvider(FileStorageProperties properties) {
        this.root = Path.of(properties.getLocalRoot()).toAbsolutePath().normalize();
        this.publicUrlBase = trimTrailingSlash(properties.getLocalPublicUrl());
    }

    @Override
    public void store(String key, InputStream input, String contentType) throws IOException {
        Path target = pathFor(key);
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), ".upload-", ".tmp");
        try {
            Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Override
    public void delete(String key) throws IOException {
        Files.deleteIfExists(pathFor(key));
    }

    @Override
    public String publicUrl(String key) {
        validateKey(key);
        return publicUrlBase + "/" + key;
    }

    private Path pathFor(String key) throws IOException {
        try {
            validateKey(key);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Storage key must stay below the configured root", exception);
        }
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw new IOException("Storage key must stay below the configured root");
        }
        return target;
    }

    private static void validateKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Storage key is required");
        }
        Path relative = Path.of(key);
        if (relative.isAbsolute() || relative.normalize().startsWith("..")) {
            throw new IllegalArgumentException("Storage key must be relative");
        }
    }

    private static String trimTrailingSlash(String value) {
        String configured = value == null || value.isBlank() ? "/uploads" : value;
        return configured.length() > 1 && configured.endsWith("/")
                ? configured.substring(0, configured.length() - 1)
                : configured;
    }
}
