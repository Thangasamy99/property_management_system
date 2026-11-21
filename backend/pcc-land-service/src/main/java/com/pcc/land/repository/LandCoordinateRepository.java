package com.pcc.land.repository;

import com.pcc.land.entity.LandCoordinate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LandCoordinateRepository extends JpaRepository<LandCoordinate, Long> {
    List<LandCoordinate> findByParcelIdOrderBySequence(Long parcelId);
    void deleteByParcelId(Long parcelId);
}
