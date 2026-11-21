package com.pcc.document.service;

import com.pcc.document.dto.request.AssociationRequest;
import com.pcc.document.dto.request.DocumentUpdateRequest;
import com.pcc.document.dto.response.*;
import com.pcc.document.entity.*;
import com.pcc.document.enums.DocStatus;
import com.pcc.document.enums.EntityType;
import com.pcc.document.exception.BusinessRuleException;
import com.pcc.document.exception.ResourceNotFoundException;
import com.pcc.document.integration.ExternalEntityValidator;
import com.pcc.document.repository.*;
import com.pcc.document.security.UserPrincipal;
import com.pcc.document.storage.DocumentStorage;
import com.pcc.document.storage.StorageProperties;
import com.pcc.document.util.FileTypeValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documents;
    private final DocumentVersionRepository versions;
    private final DocumentAssociationRepository associations;
    private final DocumentAuditLogRepository auditLogs;
    private final DocumentCategoryRepository categories;
    private final DocumentStorage storage;
    private final StorageProperties storageProps;
    private final FileTypeValidator typeValidator;
    private final ExternalEntityValidator externalValidator;

    // ---------- upload ----------

    @Transactional
    public DocumentResponse upload(MultipartFile file, Long categoryId, String description,
                                   LocalDate documentDate, LocalDate expiryDate,
                                   List<AssociationRequest> initialAssociations,
                                   UserPrincipal actor) throws Exception {
        if (file == null || file.isEmpty()) throw new BusinessRuleException("No file provided");
        if (file.getSize() > storageProps.getMaxFileSize())
            throw new BusinessRuleException("File exceeds maximum size of "
                    + storageProps.getMaxFileSize() + " bytes");

        DocumentCategory category = categories.findById(categoryId)
                .filter(DocumentCategory::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (category.isRequiresExpiry() && expiryDate == null)
            throw new BusinessRuleException("Category '" + category.getName() + "' requires an expiry date");

        String mime = validateAndSniff(file);
        String storageKey = UUID.randomUUID().toString().replace("-", "");
        long storedSize;
        try (InputStream in = file.getInputStream()) {
            storedSize = storage.store(in, storageKey);
        }
        if (!storage.exists(storageKey)) {
            storage.deleteQuietly(storageKey);
            throw new IllegalStateException("Stored file failed verification");
        }

        Document doc = new Document();
        doc.setDocumentRef(nextDocumentRef());
        doc.setCategory(category);
        doc.setOriginalFilename(sanitizeFilename(file.getOriginalFilename()));
        doc.setMimeType(mime);
        doc.setFileSize(storedSize);
        doc.setDescription(description);
        doc.setDocumentDate(documentDate);
        doc.setExpiryDate(expiryDate);
        doc.setStatus(DocStatus.ACTIVE);
        doc.setUploadedById(actor.getId());
        doc.setUploadedByName(actor.getUsername());

        try {
            doc = documents.saveAndFlush(doc);
        } catch (DataIntegrityViolationException e) {
            storage.deleteQuietly(storageKey);   // duplicate ref race — clean the file
            throw new IllegalStateException("Document reference collision — retry");
        } catch (RuntimeException e) {
            storage.deleteQuietly(storageKey);
            throw e;
        }

        DocumentVersion v = new DocumentVersion();
        v.setDocument(doc);
        v.setVersionNumber(1);
        v.setStorageKey(storageKey);
        v.setFileSize(storedSize);
        v.setMimeType(mime);
        v.setUploadedById(actor.getId());
        v.setUploadedByName(actor.getUsername());
        try {
            versions.save(v);
        } catch (RuntimeException e) {
            storage.deleteQuietly(storageKey);
            throw e;
        }

        audit(doc, "UPLOAD", actor, "v1 " + mime + " " + storedSize + "B");
        if (initialAssociations != null) {
            for (AssociationRequest a : initialAssociations) addAssociation(doc, a, actor);
        }
        return toResponse(doc);
    }

    // ---------- versions ----------

    @Transactional
    public VersionResponse uploadVersion(Long documentId, MultipartFile file, UserPrincipal actor)
            throws Exception {
        Document doc = active(documentId);
        if (file == null || file.isEmpty()) throw new BusinessRuleException("No file provided");
        if (file.getSize() > storageProps.getMaxFileSize())
            throw new BusinessRuleException("File exceeds maximum size");

        String mime = validateAndSniff(file);
        String storageKey = UUID.randomUUID().toString().replace("-", "");
        long storedSize;
        try (InputStream in = file.getInputStream()) {
            storedSize = storage.store(in, storageKey);
        }
        if (!storage.exists(storageKey)) {
            storage.deleteQuietly(storageKey);
            throw new IllegalStateException("Stored file failed verification");
        }

        int next = doc.getCurrentVersion() + 1;
        DocumentVersion v = new DocumentVersion();
        v.setDocument(doc);
        v.setVersionNumber(next);
        v.setStorageKey(storageKey);
        v.setFileSize(storedSize);
        v.setMimeType(mime);
        v.setUploadedById(actor.getId());
        v.setUploadedByName(actor.getUsername());
        try {
            versions.save(v);
        } catch (RuntimeException e) {
            storage.deleteQuietly(storageKey);
            throw e;
        }
        doc.setCurrentVersion(next);
        doc.setUpdatedAt(Instant.now());
        documents.save(doc);
        audit(doc, "NEW_VERSION", actor, "v" + next + " " + mime);
        return toVersionResponse(v, true);
    }

    @Transactional(readOnly = true)
    public List<VersionResponse> versions(Long documentId) {
        Document doc = get(documentId);
        return versions.findByDocumentIdOrderByVersionNumberDesc(doc.getId()).stream()
                .map(v -> toVersionResponse(v, v.getVersionNumber() == doc.getCurrentVersion()))
                .toList();
    }

    // ---------- download / preview ----------

    public record FilePayload(Resource resource, String filename, String mimeType, long size) {}

    @Transactional
    public FilePayload download(Long documentId, Integer versionNumber, boolean preview,
                                UserPrincipal actor) throws Exception {
        Document doc = get(documentId);
        DocumentVersion v = resolveVersion(doc, versionNumber);
        Path path = storage.pathFor(v.getStorageKey());
        if (!Files.exists(path)) throw new IllegalStateException("Stored file missing");
        audit(doc, preview ? "PREVIEW" : "DOWNLOAD", actor, "v" + v.getVersionNumber());
        return new FilePayload(new InputStreamResource(Files.newInputStream(path)),
                doc.getOriginalFilename(), v.getMimeType(), v.getFileSize());
    }

    // ---------- metadata ----------

    @Transactional(readOnly = true)
    public DocumentResponse detail(Long id) { return toResponse(get(id)); }

    @Transactional(readOnly = true)
    public DocumentResponse detailByRef(String ref) {
        return toResponse(documents.findByDocumentRef(ref)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found")));
    }

    @Transactional
    public DocumentResponse update(Long id, DocumentUpdateRequest req, UserPrincipal actor) {
        Document doc = active(id);
        if (req.categoryId() != null) {
            DocumentCategory c = categories.findById(req.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            doc.setCategory(c);
        }
        doc.setOriginalFilename(sanitizeFilename(req.originalFilename()));
        doc.setDescription(req.description());
        doc.setDocumentDate(req.documentDate());
        doc.setExpiryDate(req.expiryDate());
        doc.setUpdatedAt(Instant.now());
        audit(doc, "UPDATE", actor, "metadata");
        return toResponse(documents.save(doc));
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> search(DocStatus status, Long categoryId, String domain,
                                         Boolean expired, String q, Instant from, Instant to,
                                         Pageable pageable) {
        String like = (q == null || q.isBlank()) ? null : "%" + q.toLowerCase(Locale.ROOT) + "%";
        return documents.search(status, categoryId, domain, expired, like, from, to, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> forEntity(EntityType type, String ref) {
        return documents.findForEntity(type, ref).stream().map(this::toResponse).toList();
    }

    // ---------- associations ----------

    @Transactional
    public AssociationResponse associate(Long documentId, AssociationRequest req, UserPrincipal actor) {
        Document doc = active(documentId);
        return toAssociationResponse(addAssociation(doc, req, actor));
    }

    @Transactional
    public void removeAssociation(Long documentId, Long associationId, UserPrincipal actor) {
        Document doc = active(documentId);
        DocumentAssociation a = associations.findById(associationId)
                .filter(x -> x.getDocument().getId().equals(doc.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Association not found"));
        associations.delete(a);
        audit(doc, "DISASSOCIATE", actor, a.getEntityType() + ":" + a.getEntityRef());
    }

    @Transactional(readOnly = true)
    public List<AssociationResponse> listAssociations(Long documentId) {
        Document doc = get(documentId);
        return associations.findByDocumentId(doc.getId()).stream()
                .map(this::toAssociationResponse).toList();
    }

    // ---------- archive / restore ----------

    @Transactional
    public DocumentResponse archive(Long id, UserPrincipal actor) {
        Document doc = active(id);
        doc.setStatus(DocStatus.ARCHIVED);
        doc.setUpdatedAt(Instant.now());
        audit(doc, "ARCHIVE", actor, null);
        return toResponse(documents.save(doc));
    }

    @Transactional
    public DocumentResponse restore(Long id, UserPrincipal actor) {
        Document doc = get(id);
        if (doc.getStatus() != DocStatus.ARCHIVED)
            throw new BusinessRuleException("Document is not archived");
        doc.setStatus(DocStatus.ACTIVE);
        doc.setUpdatedAt(Instant.now());
        audit(doc, "RESTORE", actor, null);
        return toResponse(documents.save(doc));
    }

    // ---------- expiry ----------

    @Transactional(readOnly = true)
    public ExpirySummaryResponse expirySummary() {
        LocalDate today = LocalDate.now();
        return new ExpirySummaryResponse(
                documents.countExpired(today),
                documents.countExpiringBetween(today, today.plusDays(30)),
                documents.countExpiringBetween(today.plusDays(31), today.plusDays(60)),
                documents.countNoExpiry());
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> expiryList(String bucket) {
        LocalDate today = LocalDate.now();
        List<Document> list = switch (bucket == null ? "all" : bucket) {
            case "expired" -> documents.expired(today);
            case "30" -> documents.expiringBetween(today, today.plusDays(30));
            case "60" -> documents.expiringBetween(today.plusDays(31), today.plusDays(60));
            case "none" -> documents.noExpiry();
            default -> throw new BusinessRuleException("Unknown expiry bucket: " + bucket);
        };
        return list.stream().map(this::toResponse).toList();
    }

    // ---------- audit ----------

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> auditHistory(Long documentId, Pageable pageable) {
        Document doc = get(documentId);
        return auditLogs.findByDocumentIdOrderByCreatedAtDesc(doc.getId(), pageable)
                .map(a -> new AuditLogResponse(a.getId(), a.getAction(), a.getActorId(),
                        a.getActorName(), a.getDetail(), a.getCreatedAt()));
    }

    // ---------- internals ----------

    private DocumentAssociation addAssociation(Document doc, AssociationRequest req,
                                               UserPrincipal actor) {
        EntityType type = EntityType.valueOf(req.entityType());
        String ref = req.entityRef().trim();
        if (associations.existsByDocumentIdAndEntityTypeAndEntityRef(doc.getId(), type, ref))
            throw new BusinessRuleException("Association already exists");
        externalValidator.validate(type, ref);   // throws on unknown reachable ref
        DocumentAssociation a = new DocumentAssociation();
        a.setDocument(doc);
        a.setEntityType(type);
        a.setEntityRef(ref);
        a.setAssociatedById(actor.getId());
        a.setAssociatedByName(actor.getUsername());
        DocumentAssociation saved = associations.save(a);
        audit(doc, "ASSOCIATE", actor, type + ":" + ref);
        return saved;
    }

    private DocumentVersion resolveVersion(Document doc, Integer versionNumber) {
        int v = versionNumber == null ? doc.getCurrentVersion() : versionNumber;
        return versions.findByDocumentIdAndVersionNumber(doc.getId(), v)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found"));
    }

    private String validateAndSniff(MultipartFile file) throws Exception {
        try (InputStream raw = file.getInputStream();
             BufferedInputStream head = new BufferedInputStream(raw)) {
            String detected = typeValidator.detectMime(head);
            if (detected == null) throw new BusinessRuleException("Unsupported or corrupt file content");
            String ext = extension(file.getOriginalFilename());
            // Disambiguate OLE2 (doc/xls) and ZIP (docx/xlsx) via extension —
            // the magic bytes only tell the container format.
            String mime = switch (detected) {
                case "application/octet-stream-ole2" -> switch (ext) {
                    case "doc" -> "application/msword";
                    case "xls" -> "application/vnd.ms-excel";
                    default -> throw new BusinessRuleException("OLE2 content must be .doc or .xls");
                };
                case "application/octet-stream-zip" -> switch (ext) {
                    case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                    case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    default -> throw new BusinessRuleException("ZIP-based content must be .docx or .xlsx");
                };
                default -> detected;
            };
            if (!storageProps.getAllowedMime().contains(mime))
                throw new BusinessRuleException("File type not allowed: " + mime);
            return mime;
        }
    }

    private String extension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) return "unnamed";
        String clean = name.replaceAll("[\\r\\n\\t/\\\\:*?\"<>|]", "_").trim();
        if (clean.contains("..")) clean = clean.replace("..", "_");
        return clean.length() > 255 ? clean.substring(clean.length() - 255) : clean;
    }

    private synchronized String nextDocumentRef() {
        return "DOC-" + String.format("%06d", documents.maxDocumentSequence() + 1);
    }

    private Document get(Long id) {
        return documents.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
    }

    private Document active(Long id) {
        Document doc = get(id);
        if (doc.getStatus() == DocStatus.ARCHIVED)
            throw new BusinessRuleException("Document is archived");
        return doc;
    }

    private void audit(Document doc, String action, UserPrincipal actor, String detail) {
        DocumentAuditLog a = new DocumentAuditLog();
        a.setDocument(doc);
        a.setAction(action);
        a.setActorId(actor.getId());
        a.setActorName(actor.getUsername());
        a.setDetail(detail);
        auditLogs.save(a);
    }

    private DocumentResponse toResponse(Document d) {
        boolean expired = d.getExpiryDate() != null && d.getExpiryDate().isBefore(LocalDate.now());
        return new DocumentResponse(d.getId(), d.getDocumentRef(), d.getCategory().getId(),
                d.getCategory().getName(), d.getCategory().getDomain(), d.getOriginalFilename(),
                d.getMimeType(), d.getFileSize(), d.getDescription(), d.getDocumentDate(),
                d.getExpiryDate(), d.getCurrentVersion(), d.getStatus().name(), expired,
                d.getUploadedById(), d.getUploadedByName(), d.getCreatedAt(), d.getUpdatedAt());
    }

    private VersionResponse toVersionResponse(DocumentVersion v, boolean current) {
        return new VersionResponse(v.getId(), v.getVersionNumber(), v.getFileSize(), v.getMimeType(),
                current, v.getUploadedById(), v.getUploadedByName(), v.getCreatedAt());
    }

    private AssociationResponse toAssociationResponse(DocumentAssociation a) {
        return new AssociationResponse(a.getId(), a.getEntityType().name(), a.getEntityRef(),
                a.getAssociatedByName(), a.getCreatedAt());
    }
}
