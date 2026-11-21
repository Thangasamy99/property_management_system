package com.pcc.land.controller;

import com.pcc.land.dto.response.PageResponse;
import com.pcc.land.dto.response.TransactionResponse;
import com.pcc.land.enums.TransactionType;
import com.pcc.land.service.LandTransactionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/land-transactions")
@RequiredArgsConstructor
@Tag(name = "Land Transactions")
@SecurityRequirement(name = "bearerAuth")
public class LandTransactionController {

    private final LandTransactionService transactionService;

    @GetMapping
    @PreAuthorize("hasAuthority('LAND_VIEW')")
    public PageResponse<TransactionResponse> list(
            @RequestParam(required = false) Long parcelId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return transactionService.list(parcelId, type, search, page, size);
    }

    @GetMapping("/by-parcel/{parcelId}")
    @PreAuthorize("hasAuthority('LAND_VIEW')")
    public List<TransactionResponse> byParcel(@PathVariable Long parcelId) {
        return transactionService.byParcel(parcelId);
    }
}
