package com.pcc.document.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcc.document.dto.request.AssociationRequest;
import com.pcc.document.dto.request.DocumentUpdateRequest;
import com.pcc.document.dto.response.*;
import com.pcc.document.enums.DocStatus;
import com.pcc.document.enums.EntityType;
import com.pcc.document.security.UserPrincipal;
import com.pcc.document.service.DocumentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Documents")
@SecurityRequirement(name = "bearerAuth")
public class DocumentController {

    private final DocumentService service;
    private final ObjectMapper mapper = new ObjectMapper();

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('DOCUMENT_UPLOAD')")
    public DocumentResponse upload(@RequestPart("file") MultipartFile file,
                                   @RequestParam Long categoryId,
                                   @RequestParam(required = false) String description,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate documentDate,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
                                   @RequestParam(required = false) String associations,
                                   @AuthenticationPrincipal UserPrincipal actor) throws Exception {
        List<AssociationRequest> list = associations == null || associations.isBlank() ? List.of()
                : mapper.readValue(associations,
                        mapper.getTypeFactory().constructCollectionType(List.class, AssociationRequest.class));
        return service.upload(file, categoryId, description, documentDate, expiryDate, list, actor);
    }

    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAuthority('DOCUMENT_VIEW')")
    public DocumentResponse detail(@PathVariable Long id) { return service.detail(id); }

    @GetMapping
    @PreAuthorize("hasAuthority('DOCUMENT_VIEW')")
    public PageResponse<DocumentResponse> search(
            @RequestParam(required = false) DocStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) Boolean expired,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        var parts = sort.split(",");
        Sort s = parts.length == 2 && parts[1].equalsIgnoreCase("asc")
                ? Sort.by(parts[0]).ascending() : Sort.by(parts[0]).descending();
        return PageResponse.of(service.search(status, categoryId, domain, expired, q,
                from, to, PageRequest.of(page, size, s)));
    }

    @PutMapping("/{id:\\d+}")
    @PreAuthorize("hasAuthority('DOCUMENT_UPDATE')")
    public DocumentResponse update(@PathVariable Long id,
                                   @RequestBody @jakarta.validation.Valid DocumentUpdateRequest req,
                                   @AuthenticationPrincipal UserPrincipal actor) {
        return service.update(id, req, actor);
    }

    @PostMapping("/{id:\\d+}/versions")
    @PreAuthorize("hasAuthority('DOCUMENT_VERSION_UPLOAD')")
    public VersionResponse uploadVersion(@PathVariable Long id,
                                         @RequestPart("file") MultipartFile file,
                                         @AuthenticationPrincipal UserPrincipal actor) throws Exception {
        return service.uploadVersion(id, file, actor);
    }

    @GetMapping("/{id:\\d+}/versions")
    @PreAuthorize("hasAuthority('DOCUMENT_VERSION_VIEW')")
    public List<VersionResponse> versions(@PathVariable Long id) { return service.versions(id); }

    @GetMapping("/{id:\\d+}/download")
    @PreAuthorize("hasAuthority('DOCUMENT_DOWNLOAD')")
    public ResponseEntity<org.springframework.core.io.Resource> download(
            @PathVariable Long id, @RequestParam(required = false) Integer version,
            @AuthenticationPrincipal UserPrincipal actor) throws Exception {
        return file(service.download(id, version, false, actor), true);
    }

    @GetMapping("/{id:\\d+}/preview")
    @PreAuthorize("hasAuthority('DOCUMENT_PREVIEW')")
    public ResponseEntity<org.springframework.core.io.Resource> preview(
            @PathVariable Long id, @RequestParam(required = false) Integer version,
            @AuthenticationPrincipal UserPrincipal actor) throws Exception {
        return file(service.download(id, version, true, actor), false);
    }

    @PostMapping("/{id:\\d+}/archive")
    @PreAuthorize("hasAuthority('DOCUMENT_ARCHIVE')")
    public DocumentResponse archive(@PathVariable Long id,
                                    @AuthenticationPrincipal UserPrincipal actor) {
        return service.archive(id, actor);
    }

    @PostMapping("/{id:\\d+}/restore")
    @PreAuthorize("hasAuthority('DOCUMENT_RESTORE')")
    public DocumentResponse restore(@PathVariable Long id,
                                    @AuthenticationPrincipal UserPrincipal actor) {
        return service.restore(id, actor);
    }

    @PostMapping("/{id:\\d+}/associations")
    @PreAuthorize("hasAuthority('DOCUMENT_ASSOCIATE')")
    public AssociationResponse associate(@PathVariable Long id,
                                         @RequestBody @jakarta.validation.Valid AssociationRequest req,
                                         @AuthenticationPrincipal UserPrincipal actor) {
        return service.associate(id, req, actor);
    }

    @GetMapping("/{id:\\d+}/associations")
    @PreAuthorize("hasAuthority('DOCUMENT_VIEW')")
    public List<AssociationResponse> associations(@PathVariable Long id) {
        return service.listAssociations(id);
    }

    @DeleteMapping("/{id:\\d+}/associations/{associationId}")
    @PreAuthorize("hasAuthority('DOCUMENT_ASSOCIATE')")
    public void removeAssociation(@PathVariable Long id, @PathVariable Long associationId,
                                  @AuthenticationPrincipal UserPrincipal actor) {
        service.removeAssociation(id, associationId, actor);
    }

    @GetMapping("/{id:\\d+}/audit-history")
    @PreAuthorize("hasAuthority('DOCUMENT_AUDIT_VIEW')")
    public PageResponse<AuditLogResponse> auditHistory(@PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(service.auditHistory(id, PageRequest.of(page, size)));
    }

    @GetMapping("/expiry")
    @PreAuthorize("hasAuthority('DOCUMENT_EXPIRY_VIEW')")
    public List<DocumentResponse> expiryList(@RequestParam(defaultValue = "expired") String bucket) {
        return service.expiryList(bucket);
    }

    @GetMapping("/expiry/summary")
    @PreAuthorize("hasAuthority('DOCUMENT_EXPIRY_VIEW')")
    public ExpirySummaryResponse expirySummary() { return service.expirySummary(); }

    @GetMapping("/for-entity")
    @PreAuthorize("hasAuthority('DOCUMENT_VIEW')")
    public List<DocumentResponse> forEntity(@RequestParam EntityType type,
                                            @RequestParam String ref) {
        return service.forEntity(type, ref);
    }

    private ResponseEntity<org.springframework.core.io.Resource> file(
            DocumentService.FilePayload payload, boolean attachment) {
        String encoded = URLEncoder.encode(payload.filename(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        String disposition = (attachment ? "attachment" : "inline")
                + "; filename*=UTF-8''" + encoded;
        MediaType type = payload.mimeType().startsWith("image/")
                || payload.mimeType().equals("application/pdf")
                ? MediaType.parseMediaType(payload.mimeType()) : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(attachment ? MediaType.APPLICATION_OCTET_STREAM : type)
                .contentLength(payload.size())
                .body(payload.resource());
    }
}
