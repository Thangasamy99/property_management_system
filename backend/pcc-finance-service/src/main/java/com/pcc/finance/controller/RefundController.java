package com.pcc.finance.controller;

import com.pcc.finance.dto.request.DecisionRequest;
import com.pcc.finance.dto.request.RefundRequest;
import com.pcc.finance.dto.response.PageResponse;
import com.pcc.finance.dto.response.RefundResponse;
import com.pcc.finance.enums.RefundStatus;
import com.pcc.finance.service.RefundService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/refunds")
@RequiredArgsConstructor
@Tag(name = "Refunds")
@SecurityRequirement(name = "bearerAuth")
public class RefundController {

    private final RefundService refundService;

    @GetMapping
    @PreAuthorize("hasAuthority('REFUND_VIEW')")
    public PageResponse<RefundResponse> list(
            @RequestParam(required = false) RefundStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return refundService.list(status, search, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('REFUND_VIEW')")
    public RefundResponse get(@PathVariable Long id) {
        return refundService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('REFUND_CREATE')")
    public RefundResponse request(@Valid @RequestBody RefundRequest req) {
        return refundService.request(req);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('REFUND_APPROVE')")
    public RefundResponse approve(@PathVariable Long id) {
        return refundService.approve(id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('REFUND_APPROVE')")
    public RefundResponse reject(@PathVariable Long id, @RequestBody(required = false) DecisionRequest req) {
        return refundService.reject(id, req == null ? null : req.note());
    }
}
