package com.pcc.finance.service;

import com.pcc.finance.dto.request.ExpensePayRequest;
import com.pcc.finance.dto.request.ExpenseRequest;
import com.pcc.finance.dto.response.ExpenseResponse;
import com.pcc.finance.dto.response.PageResponse;
import com.pcc.finance.entity.Expense;
import com.pcc.finance.entity.Payment;
import com.pcc.finance.enums.ExpenseStatus;
import com.pcc.finance.enums.LedgerTxType;
import com.pcc.finance.enums.PaymentStatus;
import com.pcc.finance.enums.PaymentType;
import com.pcc.finance.exception.BusinessRuleException;
import com.pcc.finance.exception.ResourceNotFoundException;
import com.pcc.finance.repository.ExpenseRepository;
import com.pcc.finance.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final PaymentRepository paymentRepository;
    private final PostingService posting;
    private final FinanceSupport support;

    @Transactional
    public ExpenseResponse create(ExpenseRequest req) {
        Expense e = new Expense();
        e.setCategory(req.category());
        e.setDescription(req.description());
        e.setAmount(req.amount());
        e.setExpenseDate(req.expenseDate());
        e.setDepartment(req.department());
        e.setVendorName(req.vendorName());
        e.setVendorReference(req.vendorReference());
        e.setDocumentReference(req.documentReference());
        e.setCreatedById(support.actorId());
        e.setCreatedByName(support.actorName());
        e.setExpenseRef(FinanceSupport.ref("EXP-", expenseRepository.maxExpenseSequence() + 1));
        e = expenseRepository.save(e);
        support.audit("EXPENSE", e.getId(), "CREATED", e.getDescription());
        return ExpenseResponse.of(e);
    }

    @Transactional
    public ExpenseResponse approve(Long id) {
        Expense e = expense(id);
        if (e.getStatus() != ExpenseStatus.PENDING_APPROVAL) {
            throw new BusinessRuleException("Only pending expenses can be approved");
        }
        e.setStatus(ExpenseStatus.APPROVED);
        e.setApprovedById(support.actorId());
        e.setApprovedByName(support.actorName());
        e.setApprovedAt(Instant.now());
        expenseRepository.save(e);
        support.audit("EXPENSE", e.getId(), "APPROVED", e.getExpenseRef());
        return ExpenseResponse.of(e);
    }

    @Transactional
    public ExpenseResponse reject(Long id, String reason) {
        Expense e = expense(id);
        if (e.getStatus() != ExpenseStatus.PENDING_APPROVAL) {
            throw new BusinessRuleException("Only pending expenses can be rejected");
        }
        e.setStatus(ExpenseStatus.REJECTED);
        e.setRejectionReason(reason);
        e.setApprovedById(support.actorId());
        e.setApprovedByName(support.actorName());
        e.setApprovedAt(Instant.now());
        expenseRepository.save(e);
        support.audit("EXPENSE", e.getId(), "REJECTED", reason);
        return ExpenseResponse.of(e);
    }

    /** Pay an approved expense: creates a POSTED payout payment + DEBIT ledger entry. */
    @Transactional
    public ExpenseResponse pay(Long id, ExpensePayRequest req) {
        Expense e = expense(id);
        if (e.getStatus() != ExpenseStatus.APPROVED) {
            throw new BusinessRuleException(
                    "Expense must be approved before payment (current: " + e.getStatus() + ")");
        }
        Payment p = new Payment();
        p.setPaymentType(PaymentType.EXPENSE_PAYOUT);
        p.setPayerName(e.getVendorName() != null ? e.getVendorName() : "PCC Expense");
        p.setPayerReference(e.getVendorReference());
        p.setAmount(e.getAmount());
        p.setPaymentDate(req.paidDate());
        p.setPaymentMethod(support.method(req.methodCode()));
        p.setPaymentReference(req.paymentReference());
        p.setAccount(support.account(req.accountId()));
        p.setStatus(PaymentStatus.POSTED);
        p.setReceivedById(support.actorId());
        p.setReceivedByName(support.actorName());
        p.setPostedById(support.actorId());
        p.setPostedByName(support.actorName());
        p.setPostedAt(Instant.now());
        p.setNotes("Expense payout " + e.getExpenseRef());
        p.setPaymentRef(FinanceSupport.ref("PAY-", paymentRepository.maxPaymentSequence() + 1));
        p = paymentRepository.save(p);

        posting.debit(p, LedgerTxType.EXPENSE_PAYOUT, p.getAmount(), e.getExpenseRef(),
                "Expense payment " + e.getCategory());

        e.setPayment(p);
        e.setPaymentMethod(p.getPaymentMethod());
        e.setPaymentReference(req.paymentReference());
        e.setPaidDate(req.paidDate());
        e.setPaidById(support.actorId());
        e.setPaidByName(support.actorName());
        e.setStatus(ExpenseStatus.PAID);
        expenseRepository.save(e);
        support.audit("EXPENSE", e.getId(), "PAID", p.getPaymentRef());
        return ExpenseResponse.of(e);
    }

    @Transactional
    public ExpenseResponse cancel(Long id) {
        Expense e = expense(id);
        if (e.getStatus() == ExpenseStatus.PAID) {
            throw new BusinessRuleException("Paid expenses cannot be cancelled; use a reversal");
        }
        e.setStatus(ExpenseStatus.CANCELLED);
        expenseRepository.save(e);
        support.audit("EXPENSE", e.getId(), "CANCELLED", null);
        return ExpenseResponse.of(e);
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> list(ExpenseStatus status, String category,
                                              LocalDate from, LocalDate to, String q,
                                              int page, int size) {
        String like = q == null || q.isBlank() ? null : "%" + q.toLowerCase() + "%";
        return PageResponse.of(
                expenseRepository.search(status, category, from, to, like, PageRequest.of(page, size)),
                ExpenseResponse::of);
    }

    @Transactional(readOnly = true)
    public ExpenseResponse get(Long id) {
        return ExpenseResponse.of(expense(id));
    }

    private Expense expense(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
    }
}
