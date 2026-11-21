package com.pcc.finance.service;

import com.pcc.finance.dto.request.IncomeRequest;
import com.pcc.finance.dto.response.IncomeResponse;
import com.pcc.finance.dto.response.PageResponse;
import com.pcc.finance.entity.OtherIncome;
import com.pcc.finance.entity.Payment;
import com.pcc.finance.enums.IncomeStatus;
import com.pcc.finance.enums.LedgerTxType;
import com.pcc.finance.enums.PaymentStatus;
import com.pcc.finance.enums.PaymentType;
import com.pcc.finance.exception.BusinessRuleException;
import com.pcc.finance.exception.ResourceNotFoundException;
import com.pcc.finance.repository.OtherIncomeRepository;
import com.pcc.finance.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class OtherIncomeService {

    private final OtherIncomeRepository incomeRepository;
    private final PaymentRepository paymentRepository;
    private final PostingService posting;
    private final FinanceSupport support;

    @Transactional
    public IncomeResponse create(IncomeRequest req) {
        OtherIncome i = new OtherIncome();
        i.setCategory(req.category());
        i.setDescription(req.description());
        i.setAmount(req.amount());
        i.setIncomeDate(req.incomeDate());
        i.setPaymentMethod(support.method(req.methodCode()));
        i.setExternalReference(req.externalReference());
        i.setDocumentReference(req.documentReference());
        i.setPayerName(req.payerName());
        i.setCreatedById(support.actorId());
        i.setCreatedByName(support.actorName());
        i.setIncomeRef(FinanceSupport.ref("INC-", incomeRepository.maxIncomeSequence() + 1));
        i = incomeRepository.save(i);
        support.audit("OTHER_INCOME", i.getId(), "CREATED", i.getDescription());
        return IncomeResponse.of(i);
    }

    @Transactional
    public IncomeResponse approve(Long id) {
        OtherIncome i = income(id);
        if (i.getStatus() != IncomeStatus.RECORDED) {
            throw new BusinessRuleException("Only recorded income can be approved");
        }
        i.setStatus(IncomeStatus.APPROVED);
        i.setApprovedById(support.actorId());
        i.setApprovedByName(support.actorName());
        i.setApprovedAt(Instant.now());

        // Approving an income records the actual receipt of funds.
        Payment p = new Payment();
        p.setPaymentType(PaymentType.OTHER_INCOME);
        p.setPayerName(i.getPayerName() != null ? i.getPayerName() : i.getCategory());
        p.setAmount(i.getAmount());
        p.setPaymentDate(i.getIncomeDate());
        p.setPaymentMethod(i.getPaymentMethod());
        p.setPaymentReference(i.getExternalReference());
        p.setStatus(PaymentStatus.POSTED);
        p.setReceivedById(i.getCreatedById());
        p.setReceivedByName(i.getCreatedByName());
        p.setPostedById(support.actorId());
        p.setPostedByName(support.actorName());
        p.setPostedAt(Instant.now());
        p.setPaymentRef(FinanceSupport.ref("PAY-", paymentRepository.maxPaymentSequence() + 1));
        p = paymentRepository.save(p);

        posting.credit(p, LedgerTxType.RECEIPT, i.getIncomeRef(), "Other income " + i.getCategory());
        posting.issueReceipt(p, i.getIncomeRef());

        i.setPayment(p);
        i.setStatus(IncomeStatus.POSTED);
        incomeRepository.save(i);
        support.audit("OTHER_INCOME", i.getId(), "APPROVED_AND_POSTED", p.getPaymentRef());
        return IncomeResponse.of(i);
    }

    @Transactional
    public IncomeResponse reject(Long id, String reason) {
        OtherIncome i = income(id);
        if (i.getStatus() != IncomeStatus.RECORDED) {
            throw new BusinessRuleException("Only recorded income can be rejected");
        }
        i.setStatus(IncomeStatus.REJECTED);
        i.setRejectionReason(reason);
        i.setApprovedById(support.actorId());
        i.setApprovedByName(support.actorName());
        i.setApprovedAt(Instant.now());
        incomeRepository.save(i);
        support.audit("OTHER_INCOME", i.getId(), "REJECTED", reason);
        return IncomeResponse.of(i);
    }

    @Transactional(readOnly = true)
    public PageResponse<IncomeResponse> list(IncomeStatus status, String category,
                                             LocalDate from, LocalDate to, String q,
                                             int page, int size) {
        String like = q == null || q.isBlank() ? null : "%" + q.toLowerCase() + "%";
        return PageResponse.of(
                incomeRepository.search(status, category, from, to, like, PageRequest.of(page, size)),
                IncomeResponse::of);
    }

    @Transactional(readOnly = true)
    public IncomeResponse get(Long id) {
        return IncomeResponse.of(income(id));
    }

    private OtherIncome income(Long id) {
        return incomeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Income record not found"));
    }
}
