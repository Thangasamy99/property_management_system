package com.pcc.document.repository;

import com.pcc.document.entity.Document;
import com.pcc.document.enums.DocStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByDocumentRef(String ref);

    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(document_ref,5) AS UNSIGNED)),0) "
            + "FROM documents WHERE document_ref LIKE 'DOC-%'", nativeQuery = true)
    long maxDocumentSequence();

    @Query("SELECT d FROM Document d WHERE (:status IS NULL OR d.status = :status) "
            + "AND (:categoryId IS NULL OR d.category.id = :categoryId) "
            + "AND (:domain IS NULL OR d.category.domain = :domain) "
            + "AND (:expired IS NULL OR (:expired = true AND d.expiryDate < CURRENT_DATE) "
            + "     OR (:expired = false AND d.expiryDate >= CURRENT_DATE)) "
            + "AND (:like IS NULL OR LOWER(d.documentRef) LIKE :like "
            + "     OR LOWER(d.originalFilename) LIKE :like OR LOWER(d.description) LIKE :like) "
            + "AND (CAST(:from AS date) IS NULL OR d.createdAt >= :from) "
            + "AND (CAST(:to AS date) IS NULL OR d.createdAt < :to)")
    Page<Document> search(@Param("status") DocStatus status, @Param("categoryId") Long categoryId,
                          @Param("domain") String domain, @Param("expired") Boolean expired,
                          @Param("like") String like, @Param("from") java.time.Instant from,
                          @Param("to") java.time.Instant to, Pageable pageable);

    @Query("SELECT d FROM Document d JOIN DocumentAssociation a ON a.document = d "
            + "WHERE a.entityType = :type AND a.entityRef = :ref ORDER BY d.createdAt DESC")
    List<Document> findForEntity(@Param("type") com.pcc.document.enums.EntityType type,
                                 @Param("ref") String ref);

    // ---- expiry summaries ----
    @Query("SELECT d FROM Document d WHERE d.status = 'ACTIVE' AND d.expiryDate IS NOT NULL "
            + "AND d.expiryDate < :today ORDER BY d.expiryDate")
    List<Document> expired(@Param("today") LocalDate today);

    @Query("SELECT d FROM Document d WHERE d.status = 'ACTIVE' AND d.expiryDate BETWEEN :from AND :to "
            + "ORDER BY d.expiryDate")
    List<Document> expiringBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT d FROM Document d WHERE d.status = 'ACTIVE' AND d.expiryDate IS NULL")
    List<Document> noExpiry();

    @Query("SELECT COUNT(d) FROM Document d WHERE d.status = 'ACTIVE' AND d.expiryDate < :today")
    long countExpired(@Param("today") LocalDate today);

    @Query("SELECT COUNT(d) FROM Document d WHERE d.status = 'ACTIVE' "
            + "AND d.expiryDate BETWEEN :from AND :to")
    long countExpiringBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COUNT(d) FROM Document d WHERE d.status = 'ACTIVE' AND d.expiryDate IS NULL")
    long countNoExpiry();

    long countByStatus(DocStatus status);
}
