package com.pcc.reporting.repository;

import com.pcc.reporting.entity.ReportExecutionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportExecutionLogRepository extends JpaRepository<ReportExecutionLog, Long> {
    Page<ReportExecutionLog> findAllByOrderByCreatedAtDesc(Pageable p);
}
