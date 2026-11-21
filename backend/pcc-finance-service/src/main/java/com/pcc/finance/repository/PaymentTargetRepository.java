package com.pcc.finance.repository;

import com.pcc.finance.entity.PaymentTarget;
import com.pcc.finance.enums.TargetType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentTargetRepository extends JpaRepository<PaymentTarget, Long> {

    Optional<PaymentTarget> findByTargetTypeAndExternalId(TargetType type, Long externalId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM PaymentTarget t WHERE t.targetType = :type AND t.externalId = :externalId")
    Optional<PaymentTarget> findForUpdate(@Param("type") TargetType type, @Param("externalId") Long externalId);
}
