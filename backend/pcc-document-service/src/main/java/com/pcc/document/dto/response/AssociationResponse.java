package com.pcc.document.dto.response;

import java.time.Instant;

public record AssociationResponse(Long id, String entityType, String entityRef,
                                  String associatedByName, Instant createdAt) {}
