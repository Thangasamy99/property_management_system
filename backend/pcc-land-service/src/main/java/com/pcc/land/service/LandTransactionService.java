package com.pcc.land.service;

import com.pcc.land.dto.response.PageResponse;
import com.pcc.land.dto.response.TransactionResponse;
import com.pcc.land.enums.TransactionType;
import com.pcc.land.repository.LandTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LandTransactionService {

    private final LandTransactionRepository transactionRepository;
    private final LandSupport support;

    public PageResponse<TransactionResponse> list(Long parcelId, TransactionType type,
                                                  String search, int page, int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());
        return PageResponse.of(transactionRepository.search(parcelId, type,
                LandParcelService.blank(search), pageable), TransactionResponse::from);
    }

    public List<TransactionResponse> byParcel(Long parcelId) {
        support.parcel(parcelId);
        return transactionRepository.findByParcelIdOrderByCreatedAtDesc(parcelId)
                .stream().map(TransactionResponse::from).toList();
    }
}
