package com.pcc.property.repository;

import com.pcc.property.entity.RentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RentScheduleRepository extends JpaRepository<RentSchedule, Long> {

    List<RentSchedule> findByAgreementIdOrderByPeriodStart(Long agreementId);
}
