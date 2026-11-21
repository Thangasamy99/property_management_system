package com.pcc.property.repository;

import com.pcc.property.entity.RentSchedule;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface RentScheduleRepository extends JpaRepository<RentSchedule, Long> {

    List<RentSchedule> findByAgreementIdOrderByPeriodStart(Long agreementId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RentSchedule> findWithLockById(Long id);
}
