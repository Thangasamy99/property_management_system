package com.pcc.finance.controller;

import com.pcc.finance.dto.response.SyncRecordResponse;
import com.pcc.finance.service.SyncService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sync-records")
@RequiredArgsConstructor
@Tag(name = "Sync")
@SecurityRequirement(name = "bearerAuth")
public class SyncController {

    private final SyncService syncService;

    /** All outbox records (support/audit). */
    @GetMapping
    @PreAuthorize("hasAuthority('REFUND_VIEW')")
    public List<SyncRecordResponse> list(@RequestParam(required = false) String ref) {
        return (ref == null || ref.isBlank()
                ? syncService.listAll() : syncService.listFor(ref.trim()))
                .stream().map(SyncRecordResponse::of).toList();
    }

    /** Re-enqueue any posted allocation that was never synced downstream. */
    @PostMapping("/reconcile")
    @PreAuthorize("hasAuthority('REFUND_APPROVE')")
    public java.util.Map<String, Object> reconcile() {
        return java.util.Map.of("enqueued", syncService.reconcile());
    }

    /** Manual retry of a pending/failed sync — finance approvers only. */
    @PostMapping("/{id}/retry")
    @PreAuthorize("hasAuthority('REFUND_APPROVE')")
    public SyncRecordResponse retry(@PathVariable Long id) {
        return SyncRecordResponse.of(syncService.retry(id));
    }
}
