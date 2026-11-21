package com.pcc.document.repository;

import com.pcc.document.entity.DocumentAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentAuditLogRepository extends JpaRepository<DocumentAuditLog, Long> {
    Page<DocumentAuditLog> findByDocumentIdOrderByCreatedAtDesc(Long documentId, Pageable pageable);
}
