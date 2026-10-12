package io.reconark.plugins.storagelocal;

import io.reconark.common.plugin.FileStore;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Stores files under a root folder as {@code yyyy/MM/dd/<uuid>-<name>}. Every service that reads or writes files
 * (ingestion-service, worker) must point at the same folder.
 */
public class LocalFileStore implements FileStore {

    private final Path root;

    public LocalFileStore(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public String id() {
        return "local";
    }

    @Override
    public String description() {
        return "Files in a local or shared folder: " + root;
    }

    @Override
    public String save(String fileName, InputStream content) throws IOException {
        LocalDate today = LocalDate.now();
        String key = "%d/%02d/%02d/%s-%s".formatted(
                today.getYear(), today.getMonthValue(), today.getDayOfMonth(), UUID.randomUUID(), safeName(fileName));
        Path target = resolve(key);
        Files.createDirectories(target.getParent());
        Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        return key;
    }

    @Override
    public InputStream open(String storageKey) throws IOException {
        return Files.newInputStream(resolve(storageKey));
    }

    /** Resolves a key inside the root folder and refuses keys like {@code ../../etc/passwd}. */
    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Storage key points outside the storage folder: " + key);
        }
        return path;
    }

    static String safeName(String fileName) {
        String name = fileName == null ? "file" : Path.of(fileName).getFileName().toString();
        name = name.replaceAll("[^A-Za-z0-9._-]", "_");
        return name.isBlank() ? "file" : name;
    }
}
