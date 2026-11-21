package com.pcc.reporting.repository;

import com.pcc.reporting.entity.ReportDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReportDefinitionRepository extends JpaRepository<ReportDefinition, Long> {
    Optional<ReportDefinition> findByReportKey(String key);
    List<ReportDefinition> findByActiveTrueOrderByDomainAscNameAsc();
    List<ReportDefinition> findByDomainAndActiveTrue(String domain);
}
