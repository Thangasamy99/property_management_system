package com.pcc.finance.repository;

import com.pcc.finance.entity.FinanceAccount;
import com.pcc.finance.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FinanceAccountRepository extends JpaRepository<FinanceAccount, Long> {
    List<FinanceAccount> findByAccountTypeOrderByCode(AccountType type);
    Optional<FinanceAccount> findByCode(String code);
}
