package com.pcc.property.repository;

import com.pcc.property.entity.MaintenanceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceHistoryRepository extends JpaRepository<MaintenanceHistory, Long> {

    List<MaintenanceHistory> findByRequestIdOrderByCreatedAtDesc(Long requestId);
}
