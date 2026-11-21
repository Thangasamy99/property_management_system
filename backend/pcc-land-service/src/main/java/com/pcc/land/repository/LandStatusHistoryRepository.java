package com.pcc.land.repository;

import com.pcc.land.entity.LandStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LandStatusHistoryRepository extends JpaRepository<LandStatusHistory, Long> {
    List<LandStatusHistory> findByParcelIdOrderByChangedAtDesc(Long parcelId);
}
