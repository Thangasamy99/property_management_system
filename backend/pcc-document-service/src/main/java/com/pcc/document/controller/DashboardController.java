package com.pcc.document.controller;

import com.pcc.document.dto.response.DashboardResponse;
import com.pcc.document.enums.DocStatus;
import com.pcc.document.repository.DocumentRepository;
import com.pcc.document.service.DocumentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Document Dashboard")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DocumentRepository documents;
    private final DocumentService service;

    @GetMapping
    @PreAuthorize("hasAuthority('DOCUMENT_VIEW')")
    public DashboardResponse dashboard() {
        return new DashboardResponse(
                documents.countByStatus(DocStatus.ACTIVE),
                documents.countByStatus(DocStatus.ARCHIVED),
                service.expirySummary());
    }
}
