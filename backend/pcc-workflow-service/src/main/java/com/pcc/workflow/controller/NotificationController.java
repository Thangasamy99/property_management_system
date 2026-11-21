package com.pcc.workflow.controller;

import com.pcc.workflow.dto.response.NotificationResponse;
import com.pcc.workflow.dto.response.PageResponse;
import com.pcc.workflow.security.UserPrincipal;
import com.pcc.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final WorkflowService service;

    @GetMapping
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    public PageResponse<NotificationResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal actor) {
        return PageResponse.of(service.myNotifications(actor, PageRequest.of(page, size)));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal UserPrincipal actor) {
        return Map.of("unread", service.unreadCount(actor));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    public void markRead(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal actor) {
        service.markRead(id, actor);
    }

    @PatchMapping("/read-all")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    public void markAllRead(@AuthenticationPrincipal UserPrincipal actor) {
        service.markAllRead(actor);
    }
}
