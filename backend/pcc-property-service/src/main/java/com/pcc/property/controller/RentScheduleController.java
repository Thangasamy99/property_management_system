package com.pcc.property.controller;

import com.pcc.property.dto.request.SchedulePaymentRequest;
import com.pcc.property.dto.response.RentScheduleResponse;
import com.pcc.property.service.RentScheduleService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rent-schedules")
@RequiredArgsConstructor
@Tag(name = "Rent Schedules")
@SecurityRequirement(name = "bearerAuth")
public class RentScheduleController {

    private final RentScheduleService scheduleService;

    /** Finance integration contract: sync total received for a schedule. */
    @PatchMapping("/{id}/payment")
    @PreAuthorize("hasAuthority('RENT_SCHEDULE_PAYMENT_SYNC')")
    public RentScheduleResponse recordPayment(@PathVariable Long id,
                                              @Valid @RequestBody SchedulePaymentRequest req) {
        return scheduleService.recordPayment(id, req.paidAmount());
    }
}
