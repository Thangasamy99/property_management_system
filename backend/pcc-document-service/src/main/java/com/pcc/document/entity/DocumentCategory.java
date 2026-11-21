package com.pcc.document.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "document_categories")
public class DocumentCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    /** PROPERTY | LAND | FINANCE | ADMINISTRATION */
    @Column(nullable = false, length = 20)
    private String domain;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean requiresExpiry = false;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
