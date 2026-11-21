package com.pcc.document.dto.response;

import java.time.Instant;
import java.time.LocalDate;

public record DocumentResponse(Long id, String documentRef, Long categoryId, String categoryName,
                               String categoryDomain, String originalFilename, String mimeType,
                               long fileSize, String description, LocalDate documentDate,
                               LocalDate expiryDate, int currentVersion, String status,
                               boolean expired, Long uploadedById, String uploadedByName,
                               Instant createdAt, Instant updatedAt) {}
