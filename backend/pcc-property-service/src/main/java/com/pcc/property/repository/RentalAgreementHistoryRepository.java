package com.pcc.property.repository;

import com.pcc.property.entity.RentalAgreementHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RentalAgreementHistoryRepository extends JpaRepository<RentalAgreementHistory, Long> {

    List<RentalAgreementHistory> findByAgreementIdOrderByCreatedAtDesc(Long agreementId);
}
