package com.pcc.land.repository;

import com.pcc.land.entity.LandOwnershipHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LandOwnershipHistoryRepository extends JpaRepository<LandOwnershipHistory, Long> {
    List<LandOwnershipHistory> findByParcelIdOrderByCreatedAtDesc(Long parcelId);
}
