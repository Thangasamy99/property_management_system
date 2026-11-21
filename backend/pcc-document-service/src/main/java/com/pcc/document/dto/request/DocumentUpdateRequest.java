package com.pcc.document.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record DocumentUpdateRequest(@NotBlank String originalFilename, String description,
                                    Long categoryId, LocalDate documentDate, LocalDate expiryDate) {}
