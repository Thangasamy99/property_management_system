package com.pcc.land.repository;

import com.pcc.land.entity.LandFinancePaymentReversal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LandFinancePaymentReversalRepository extends JpaRepository<LandFinancePaymentReversal, Long> {

    boolean existsByReversalRef(String reversalRef);
}
