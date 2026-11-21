package com.pcc.finance.service;

import com.pcc.finance.entity.LedgerEntry;
import com.pcc.finance.entity.Payment;
import com.pcc.finance.entity.Receipt;
import com.pcc.finance.enums.LedgerDirection;
import com.pcc.finance.enums.LedgerTxType;
import com.pcc.finance.repository.LedgerEntryRepository;
import com.pcc.finance.repository.ReceiptRepository;
import com.pcc.finance.util.NumberToWords;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Shared posting side-effects: immutable ledger entries and receipts. */
@Service
@RequiredArgsConstructor
public class PostingService {

    private final LedgerEntryRepository ledgerRepository;
    private final ReceiptRepository receiptRepository;
    private final FinanceSupport support;

    public LedgerEntry credit(Payment p, LedgerTxType type, String relatedRef, String description) {
        return write(p, type, LedgerDirection.CREDIT, p.getAmount(), relatedRef, description, null);
    }

    public LedgerEntry debit(Payment p, LedgerTxType type, BigDecimal amount,
                             String relatedRef, String description) {
        return write(p, type, LedgerDirection.DEBIT, amount, relatedRef, description, null);
    }

    public LedgerEntry adjustment(com.pcc.finance.entity.FinanceAccount account, LedgerDirection direction,
                                  BigDecimal amount, LocalDate date, String description) {
        LedgerEntry e = new LedgerEntry();
        e.setTransactionType(LedgerTxType.ADJUSTMENT);
        e.setDirection(direction);
        e.setAmount(amount);
        e.setTransactionDate(date);
        e.setSourceType("ADJUSTMENT");
        e.setAccount(account);
        e.setDescription(description);
        e.setCreatedById(support.actorId());
        e.setCreatedByName(support.actorName());
        e.setEntryRef(FinanceSupport.ref("LED-", ledgerRepository.maxLedgerSequence() + 1));
        return ledgerRepository.save(e);
    }

    /** Reversing entry mirroring an original entry's direction; marks the original REVERSED. */
    public LedgerEntry reversal(LedgerEntry original, String reason) {
        original.setPostingStatus(com.pcc.finance.enums.PostingStatus.REVERSED);
        ledgerRepository.save(original);
        LedgerEntry e = new LedgerEntry();
        e.setTransactionType(LedgerTxType.REVERSAL);
        e.setDirection(original.getDirection() == LedgerDirection.CREDIT
                ? LedgerDirection.DEBIT : LedgerDirection.CREDIT);
        e.setAmount(original.getAmount());
        e.setTransactionDate(LocalDate.now());
        e.setSourceType("REVERSAL");
        e.setSourceRef(original.getEntryRef());
        e.setPayment(original.getPayment());
        e.setAccount(original.getAccount());
        e.setPaymentMethod(original.getPaymentMethod());
        e.setReversalOfId(original.getId());
        e.setDescription("Reversal of " + original.getEntryRef() + ": " + reason);
        e.setCreatedById(support.actorId());
        e.setCreatedByName(support.actorName());
        e.setEntryRef(FinanceSupport.ref("LED-", ledgerRepository.maxLedgerSequence() + 1));
        return ledgerRepository.save(e);
    }

    private LedgerEntry write(Payment p, LedgerTxType type, LedgerDirection direction,
                              BigDecimal amount, String relatedRef, String description, Void unused) {
        LedgerEntry e = new LedgerEntry();
        e.setTransactionType(type);
        e.setDirection(direction);
        e.setAmount(amount);
        e.setTransactionDate(p.getPaymentDate());
        e.setSourceType("PAYMENT");
        e.setSourceId(p.getId());
        e.setSourceRef(p.getPaymentRef());
        e.setPayment(p);
        e.setAccount(p.getAccount());
        e.setPaymentMethod(p.getPaymentMethod().getCode());
        e.setDescription(description + (relatedRef != null ? " — " + relatedRef : ""));
        e.setCreatedById(support.actorId());
        e.setCreatedByName(support.actorName());
        e.setEntryRef(FinanceSupport.ref("LED-", ledgerRepository.maxLedgerSequence() + 1));
        return ledgerRepository.save(e);
    }

    public Receipt issueReceipt(Payment p, String relatedRef) {
        Receipt r = new Receipt();
        r.setPayment(p);
        r.setPayerName(p.getPayerName());
        r.setAmount(p.getAmount());
        r.setAmountInWords(NumberToWords.xaf(p.getAmount()));
        r.setPaymentDate(p.getPaymentDate());
        r.setPaymentMethod(p.getPaymentMethod().getCode());
        r.setPaymentReference(p.getPaymentReference());
        r.setTransactionType(p.getPaymentType().name());
        r.setRelatedReference(relatedRef);
        r.setReceivedByName(p.getReceivedByName());
        r.setIssuedById(support.actorId());
        r.setIssuedByName(support.actorName());
        r.setReceiptNo(FinanceSupport.ref("RCP-", receiptRepository.maxReceiptSequence() + 1));
        return receiptRepository.save(r);
    }
}
