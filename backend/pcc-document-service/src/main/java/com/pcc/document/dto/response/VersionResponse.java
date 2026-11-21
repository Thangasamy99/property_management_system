package com.pcc.document.dto.response;

import java.time.Instant;

public record VersionResponse(Long id, int versionNumber, long fileSize, String mimeType,
                              boolean current, Long uploadedById, String uploadedByName,
                              Instant createdAt) {}
