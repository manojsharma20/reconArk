package io.reconark.ingestion;

import io.reconark.common.model.Side;
import io.reconark.common.plugin.FileStore;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Stores the content with the FileStore plugin (computing its SHA-256 on the way) and records the upload. */
@Service
public class UploadService {

    private static final Logger log = LoggerFactory.getLogger(UploadService.class);

    private final FileStore fileStore;
    private final FileRepository repository;

    public UploadService(FileStore fileStore, FileRepository repository) {
        this.fileStore = fileStore;
        this.repository = repository;
    }

    public UploadedFile upload(String definitionId, Side side, String originalName, long size, InputStream content)
            throws IOException {
        MessageDigest sha256 = sha256();
        String storageKey;
        try (var digesting = new DigestInputStream(content, sha256)) {
            storageKey = fileStore.save(originalName, digesting);
        }
        var file = new UploadedFile(UUID.randomUUID().toString(), definitionId, side, originalName, storageKey, size,
                HexFormat.of().formatHex(sha256.digest()), null);
        repository.insert(file);
        log.info("Stored {} ({} bytes) as {} for definition {} side {}", originalName, size, file.id(), definitionId, side);
        return repository.findById(file.id()).orElseThrow();
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
