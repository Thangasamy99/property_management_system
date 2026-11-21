package com.pcc.document.repository;

import com.pcc.document.entity.DocumentCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentCategoryRepository extends JpaRepository<DocumentCategory, Long> {
    Optional<DocumentCategory> findByCode(String code);
    List<DocumentCategory> findByDomainOrderByName(String domain);
    List<DocumentCategory> findByActiveTrueOrderByDomainAscNameAsc();
}
