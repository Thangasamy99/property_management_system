package com.pcc.document.storage;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Abstraction for private binary storage. Local filesystem implementation now;
 * S3-compatible store can replace it without changing business APIs.
 */
public interface DocumentStorage {
    /** Store a stream under a generated key. Returns the stored size, or throws. */
    long store(InputStream in, String storageKey) throws Exception;
    Path pathFor(String storageKey);
    boolean exists(String storageKey);
    void deleteQuietly(String storageKey);
}
