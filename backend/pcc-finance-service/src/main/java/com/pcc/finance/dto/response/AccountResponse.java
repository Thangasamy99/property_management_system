package com.pcc.finance.dto.response;

import com.pcc.finance.entity.FinanceAccount;

import java.math.BigDecimal;

public record AccountResponse(
        Long id, String accountType, String code, String name, String description,
        BigDecimal openingBalance, BigDecimal balance, boolean active
) {
    public static AccountResponse of(FinanceAccount a, BigDecimal ledgerBalance) {
        return new AccountResponse(a.getId(), a.getAccountType().name(), a.getCode(), a.getName(),
                a.getDescription(), a.getOpeningBalance(),
                a.getOpeningBalance().add(ledgerBalance == null ? BigDecimal.ZERO : ledgerBalance),
                a.isActive());
    }
}
