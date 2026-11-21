package com.pcc.land.repository;

import com.pcc.land.entity.LandSurvey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LandSurveyRepository extends JpaRepository<LandSurvey, Long> {

    boolean existsBySurveyReference(String surveyReference);

    List<LandSurvey> findByParcelIdOrderBySurveyDateDesc(Long parcelId);

    @Query("""
            SELECT s FROM LandSurvey s JOIN s.parcel p
            WHERE p.deletedAt IS NULL
              AND (:parcelId IS NULL OR p.id = :parcelId)
              AND (:search IS NULL OR LOWER(s.surveyReference) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(s.surveyorName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(p.parcelReference) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<LandSurvey> search(@Param("parcelId") Long parcelId,
                            @Param("search") String search,
                            Pageable pageable);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(s.surveyReference, 5) AS long)), 0) FROM LandSurvey s WHERE s.surveyReference LIKE 'SRV-%'")
    long maxSurveySequence();
}
