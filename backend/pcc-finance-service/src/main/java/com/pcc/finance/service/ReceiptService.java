package com.pcc.finance.service;

import com.pcc.finance.dto.response.PageResponse;
import com.pcc.finance.dto.response.ReceiptResponse;
import com.pcc.finance.exception.ResourceNotFoundException;
import com.pcc.finance.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final ReceiptRepository receiptRepository;

    @Transactional(readOnly = true)
    public PageResponse<ReceiptResponse> list(LocalDate from, LocalDate to, String q, int page, int size) {
        String like = q == null || q.isBlank() ? null : "%" + q.toLowerCase() + "%";
        return PageResponse.of(
                receiptRepository.search(from, to, like, PageRequest.of(page, size)), ReceiptResponse::of);
    }

    @Transactional(readOnly = true)
    public ReceiptResponse get(Long id) {
        return ReceiptResponse.of(receiptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found")));
    }

    @Transactional(readOnly = true)
    public ReceiptResponse byPayment(Long paymentId) {
        return ReceiptResponse.of(receiptRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("No receipt for this payment")));
    }
}
