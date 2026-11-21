package com.pcc.document.storage;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;

/**
 * Private local filesystem storage. Files are organized under
 * root/<2-char prefix>/<key> with an atomic tmp->final move. Only
 * generated UUID keys reach this layer — never client filenames.
 */
@Slf4j
@Component
public class LocalDocumentStorage implements DocumentStorage {

    private final StorageProperties props;
    private Path root;

    public LocalDocumentStorage(StorageProperties props) { this.props = props; }

    @PostConstruct
    void init() throws Exception {
        this.root = Paths.get(props.getRoot()).toAbsolutePath().normalize();
        Files.createDirectories(root);
        log.info("Document storage root: {}", root);
    }

    @Override
    public long store(InputStream in, String storageKey) throws Exception {
        Path target = pathFor(storageKey);
        Files.createDirectories(target.getParent());
        Path tmp = target.resolveSibling(storageKey + ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(tmp, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                long size = in.transferTo(out);
                out.flush();
                Files.move(tmp, target,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                return size;
            }
        } catch (Exception e) {
            Files.deleteIfExists(tmp);
            Files.deleteIfExists(target);   // don't leave partial files behind
            throw e;
        }
    }

    @Override
    public Path pathFor(String storageKey) {
        // path traversal defense: key must already be a clean generated id
        if (!storageKey.matches("[a-zA-Z0-9._-]+") || storageKey.contains("..")) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        String prefix = storageKey.length() >= 2 ? storageKey.substring(0, 2) : "00";
        return root.resolve(prefix).resolve(storageKey);
    }

    @Override
    public boolean exists(String storageKey) { return Files.exists(pathFor(storageKey)); }

    @Override
    public void deleteQuietly(String storageKey) {
        try { Files.deleteIfExists(pathFor(storageKey)); } catch (Exception ignored) {}
    }
}
