package com.pcc.document.repository;

import com.pcc.document.entity.DocumentAssociation;
import com.pcc.document.enums.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentAssociationRepository extends JpaRepository<DocumentAssociation, Long> {
    List<DocumentAssociation> findByDocumentId(Long documentId);
    List<DocumentAssociation> findByEntityTypeAndEntityRef(EntityType type, String ref);
    boolean existsByDocumentIdAndEntityTypeAndEntityRef(Long documentId, EntityType type, String ref);
    Optional<DocumentAssociation> findByDocumentIdAndEntityTypeAndEntityRef(Long documentId, EntityType type, String ref);
}
