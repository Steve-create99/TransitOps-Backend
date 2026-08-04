package com.transitops.backend.controller;

import com.transitops.backend.dto.common.PageResponse;
import com.transitops.backend.entity.Notification;
import com.transitops.backend.entity.User;
import com.transitops.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public PageResponse<Notification> list(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return notificationService.list(user, category, search, pageable);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unread(@AuthenticationPrincipal User user) {
        return Map.of("count", notificationService.unreadCount(user));
    }

    @PatchMapping("/{id}/read")
    public Notification markRead(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return notificationService.markRead(id, user);
    }

    @PostMapping("/mark-all-read")
    public Map<String, Object> markAll(@AuthenticationPrincipal User user) {
        return notificationService.markAllRead(user);
    }

    @PatchMapping("/{id}/archive")
    public Notification archive(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return notificationService.archive(id, user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal User user) {
        notificationService.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public Notification create(@RequestBody Map<String, Object> body) {
        return notificationService.create(
                String.valueOf(body.get("title")),
                String.valueOf(body.get("message")),
                body.get("category") != null ? String.valueOf(body.get("category")) : null,
                body.get("priority") != null ? String.valueOf(body.get("priority")) : null,
                Long.valueOf(String.valueOf(body.get("userId")))
        );
    }
}
