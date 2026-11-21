package com.pcc.workflow.repository;

import com.pcc.workflow.entity.WorkflowDelegation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface WorkflowDelegationRepository extends JpaRepository<WorkflowDelegation, Long> {

    @Query("SELECT d FROM WorkflowDelegation d WHERE d.delegateId = :uid AND d.status = 'ACTIVE' "
            + "AND d.startDate <= :today AND d.endDate >= :today")
    List<WorkflowDelegation> activeFor(@Param("uid") Long uid, @Param("today") LocalDate today);

    List<WorkflowDelegation> findByDelegatorIdOrDelegateId(Long a, Long b);
}
