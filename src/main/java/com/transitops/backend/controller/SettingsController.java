package com.transitops.backend.controller;

import com.transitops.backend.dto.common.PageResponse;
import com.transitops.backend.entity.AuditLog;
import com.transitops.backend.entity.OrganizationSettings;
import com.transitops.backend.entity.User;
import com.transitops.backend.service.AuditService;
import com.transitops.backend.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SettingsController {

    private final SettingsService settingsService;
    private final AuditService auditService;

    @GetMapping
    public OrganizationSettings get() {
        return settingsService.get();
    }

    @PutMapping
    public OrganizationSettings update(@RequestBody OrganizationSettings body, @AuthenticationPrincipal User user) {
        return settingsService.update(body, user.getEmail());
    }

    @GetMapping("/users")
    public PageResponse<User> users(@PageableDefault(size = 50) Pageable pageable) {
        return PageResponse.from(settingsService.users(pageable));
    }

    @PostMapping("/users/invite")
    public User invite(@RequestBody Map<String, String> body, @AuthenticationPrincipal User user) {
        return settingsService.inviteUser(body, user.getEmail());
    }

    @PatchMapping("/users/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody Map<String, Object> body, @AuthenticationPrincipal User user) {
        return settingsService.updateUser(id, body, user.getEmail());
    }

    @GetMapping("/audit-logs")
    public PageResponse<AuditLog> auditLogs(@PageableDefault(size = 50) Pageable pageable) {
        return PageResponse.from(auditService.list(pageable));
    }
}
