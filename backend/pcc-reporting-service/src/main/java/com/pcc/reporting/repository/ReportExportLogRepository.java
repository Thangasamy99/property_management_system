package com.pcc.reporting.repository;

import com.pcc.reporting.entity.ReportExportLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportExportLogRepository extends JpaRepository<ReportExportLog, Long> {
}
