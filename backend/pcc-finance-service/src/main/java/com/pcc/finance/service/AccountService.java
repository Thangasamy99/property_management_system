package com.pcc.finance.service;

import com.pcc.finance.dto.request.AccountRequest;
import com.pcc.finance.dto.response.AccountResponse;
import com.pcc.finance.entity.FinanceAccount;
import com.pcc.finance.enums.AccountType;
import com.pcc.finance.exception.BusinessRuleException;
import com.pcc.finance.exception.DuplicateResourceException;
import com.pcc.finance.exception.ResourceNotFoundException;
import com.pcc.finance.repository.FinanceAccountRepository;
import com.pcc.finance.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final FinanceAccountRepository accountRepository;
    private final LedgerEntryRepository ledgerRepository;
    private final FinanceSupport support;

    @Transactional(readOnly = true)
    public List<AccountResponse> list(AccountType type) {
        var accounts = type == null ? accountRepository.findAll()
                : accountRepository.findByAccountTypeOrderByCode(type);
        return accounts.stream()
                .map(a -> AccountResponse.of(a, ledgerRepository.balanceForAccount(a.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse get(Long id) {
        FinanceAccount a = account(id);
        return AccountResponse.of(a, ledgerRepository.balanceForAccount(a.getId()));
    }

    @Transactional
    public AccountResponse create(AccountRequest req) {
        AccountType type = parseType(req.accountType());
        if (accountRepository.findByCode(req.code()).isPresent()) {
            throw new DuplicateResourceException("Account code already exists: " + req.code());
        }
        FinanceAccount a = new FinanceAccount();
        a.setAccountType(type);
        a.setCode(req.code());
        a.setName(req.name());
        a.setDescription(req.description());
        a.setOpeningBalance(req.openingBalance() != null ? req.openingBalance() : BigDecimal.ZERO);
        a = accountRepository.save(a);
        support.audit("ACCOUNT", a.getId(), "CREATED", a.getCode());
        return AccountResponse.of(a, BigDecimal.ZERO);
    }

    @Transactional
    public AccountResponse update(Long id, AccountRequest req) {
        FinanceAccount a = account(id);
        a.setName(req.name());
        a.setDescription(req.description());
        accountRepository.save(a);
        return AccountResponse.of(a, ledgerRepository.balanceForAccount(a.getId()));
    }

    @Transactional
    public AccountResponse toggle(Long id) {
        FinanceAccount a = account(id);
        a.setActive(!a.isActive());
        accountRepository.save(a);
        return AccountResponse.of(a, ledgerRepository.balanceForAccount(a.getId()));
    }

    private FinanceAccount account(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Finance account not found"));
    }

    private AccountType parseType(String t) {
        try {
            return AccountType.valueOf(t.toUpperCase());
        } catch (Exception e) {
            throw new BusinessRuleException("Account type must be CASH or BANK");
        }
    }
}
