package com.pcc.document.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "document_audit_logs")
public class DocumentAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id")
    private Document document;

    /** UPLOAD|NEW_VERSION|DOWNLOAD|PREVIEW|UPDATE|ASSOCIATE|DISASSOCIATE|ARCHIVE|RESTORE */
    @Column(nullable = false, length = 30)
    private String action;

    @Column(nullable = false)
    private Long actorId;

    @Column(nullable = false, length = 120)
    private String actorName;

    @Column(length = 500)
    private String detail;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
