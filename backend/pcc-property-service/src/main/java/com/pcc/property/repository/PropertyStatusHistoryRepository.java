package com.pcc.property.repository;

import com.pcc.property.entity.PropertyStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PropertyStatusHistoryRepository extends JpaRepository<PropertyStatusHistory, Long> {

    List<PropertyStatusHistory> findByPropertyIdOrderByCreatedAtDesc(Long propertyId);
}
