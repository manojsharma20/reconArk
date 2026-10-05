package io.reconark.plugins.storage.fs;

import io.reconark.spi.ObjectStore;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

/** Stores objects under a root directory. Keys are normalised and confined to the root (no path traversal). */
final class FilesystemObjectStore implements ObjectStore {

    private final Path root;

    FilesystemObjectStore(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    Path resolve(String key) {
        if (key.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Object key contains NUL");
        }
        Path p = root.resolve(key).normalize();
        if (!p.startsWith(root) || p.equals(root)) {
            throw new IllegalArgumentException("Object key escapes the store root");
        }
        return p;
    }

    @Override
    public String put(String key, InputStream content, Map<String, String> metadata) throws IOException {
        Path target = resolve(key);
        Files.createDirectories(target.getParent());
        MessageDigest sha;
        try {
            sha = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        Path tmp = Files.createTempFile(target.getParent(), ".upload-", ".tmp");
        try (OutputStream out = new DigestOutputStream(Files.newOutputStream(tmp), sha)) {
            content.transferTo(out);
        }
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return HexFormat.of().formatHex(sha.digest());
    }

    @Override
    public InputStream open(String key) throws IOException {
        return Files.newInputStream(resolve(key));
    }

    @Override
    public InputStream openRange(String key, long offset, long length) throws IOException {
        InputStream in = Files.newInputStream(resolve(key));
        in.skipNBytes(offset);
        return new BoundedInputStream(in, length);
    }

    @Override
    public boolean exists(String key) {
        return Files.exists(resolve(key));
    }

    /** Reads at most {@code limit} bytes. */
    private static final class BoundedInputStream extends FilterInputStream {
        private long remaining;

        BoundedInputStream(InputStream in, long limit) {
            super(in);
            this.remaining = limit;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int b = super.read();
            if (b >= 0) {
                remaining--;
            }
            return b;
        }

        @Override
        public int read(byte[] buf, int off, int len) throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int n = super.read(buf, off, (int) Math.min(len, remaining));
            if (n > 0) {
                remaining -= n;
            }
            return n;
        }
    }
}
