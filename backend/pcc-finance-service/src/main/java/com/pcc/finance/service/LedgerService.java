package com.pcc.finance.service;

import com.pcc.finance.dto.request.AdjustmentRequest;
import com.pcc.finance.dto.response.LedgerEntryResponse;
import com.pcc.finance.dto.response.PageResponse;
import com.pcc.finance.entity.LedgerEntry;
import com.pcc.finance.enums.LedgerDirection;
import com.pcc.finance.enums.LedgerTxType;
import com.pcc.finance.exception.BusinessRuleException;
import com.pcc.finance.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerEntryRepository ledgerRepository;
    private final PostingService posting;
    private final FinanceSupport support;

    @Transactional(readOnly = true)
    public PageResponse<LedgerEntryResponse> list(LedgerTxType type, LedgerDirection direction,
                                                  Long accountId, LocalDate from, LocalDate to,
                                                  String q, int page, int size) {
        String like = q == null || q.isBlank() ? null : "%" + q.toLowerCase() + "%";
        return PageResponse.of(
                ledgerRepository.search(type, direction, accountId, from, to, like,
                        PageRequest.of(page, size)),
                LedgerEntryResponse::of);
    }

    /** Manual adjustment entry — corrections never edit posted entries. */
    @Transactional
    public LedgerEntryResponse adjust(AdjustmentRequest req) {
        LedgerDirection dir;
        try {
            dir = LedgerDirection.valueOf(req.direction().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Direction must be CREDIT or DEBIT");
        }
        LedgerEntry e = posting.adjustment(support.account(req.accountId()), dir,
                req.amount(), req.transactionDate(), req.description());
        support.audit("LEDGER", e.getId(), "ADJUSTMENT", req.description());
        return LedgerEntryResponse.of(e);
    }
}
