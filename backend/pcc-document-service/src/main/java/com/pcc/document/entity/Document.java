package com.pcc.document.entity;

import com.pcc.document.enums.DocStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String documentRef;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private DocumentCategory category;

    @Column(nullable = false, length = 255)
    private String originalFilename;

    @Column(nullable = false, length = 120)
    private String mimeType;

    @Column(nullable = false)
    private long fileSize;

    @Column(length = 500)
    private String description;

    private LocalDate documentDate;

    private LocalDate expiryDate;

    @Column(nullable = false)
    private int currentVersion = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private DocStatus status = DocStatus.ACTIVE;

    @Column(nullable = false)
    private Long uploadedById;

    @Column(nullable = false, length = 120)
    private String uploadedByName;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
