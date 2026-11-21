package com.pcc.document.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "document_versions")
public class DocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id")
    private Document document;

    @Column(nullable = false)
    private int versionNumber;

    /** UUID storage key — never the client filename, never exposed in APIs. */
    @Column(nullable = false, length = 80)
    private String storageKey;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false, length = 120)
    private String mimeType;

    @Column(nullable = false)
    private Long uploadedById;

    @Column(nullable = false, length = 120)
    private String uploadedByName;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
