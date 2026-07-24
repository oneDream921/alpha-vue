package io.github.onedream921.alphavue.modules.file;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
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
        Path parent = createSafeParentDirectories(target);
        Path temporary = Files.createTempFile(parent, ".upload-", ".tmp");
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
        Path realRoot = realRoot();
        Path relative = Path.of(key).normalize();
        Path target = realRoot.resolve(relative).normalize();
        if (!target.startsWith(realRoot)) {
            throw new IOException("Storage key must stay below the configured root");
        }
        rejectSymlinkComponents(realRoot, relative);
        return target;
    }

    private Path realRoot() throws IOException {
        Files.createDirectories(root);
        return root.toRealPath();
    }

    private static void rejectSymlinkComponents(Path root, Path relative) throws IOException {
        Path current = root;
        for (Path component : relative) {
            current = current.resolve(component);
            if (Files.isSymbolicLink(current)) {
                throw new IOException("Storage key must not traverse symbolic links");
            }
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)
                    && !current.equals(root.resolve(relative))
                    && !Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Storage key has a non-directory parent");
            }
        }
    }

    private Path createSafeParentDirectories(Path target) throws IOException {
        Path realRoot = realRoot();
        Path parent = target.getParent();
        if (!parent.startsWith(realRoot)) {
            throw new IOException("Storage key must stay below the configured root");
        }
        Path current = realRoot;
        for (Path component : realRoot.relativize(parent)) {
            current = current.resolve(component);
            if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                Files.createDirectory(current);
            }
            if (Files.isSymbolicLink(current) || !Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Storage key must not traverse symbolic links or non-directory parents");
            }
        }
        return current;
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
