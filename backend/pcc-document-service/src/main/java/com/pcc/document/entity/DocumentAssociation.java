package com.pcc.document.entity;

import com.pcc.document.enums.EntityType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "document_associations")
public class DocumentAssociation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id")
    private Document document;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EntityType entityType;

    /** External ID/reference in the owning service — no cross-DB FK. */
    @Column(nullable = false, length = 80)
    private String entityRef;

    @Column(nullable = false)
    private Long associatedById;

    @Column(nullable = false, length = 120)
    private String associatedByName;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
