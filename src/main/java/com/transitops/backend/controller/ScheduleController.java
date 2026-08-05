package com.transitops.backend.controller;

import com.transitops.backend.dto.ScheduleDtos;
import com.transitops.backend.dto.common.PageResponse;
import com.transitops.backend.entity.User;
import com.transitops.backend.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    /** Read allowed for DRIVER (mobile companion). Mutations stay staff-only. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','DRIVER')")
    public PageResponse<ScheduleDtos.Response> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long routeId,
            @PageableDefault(size = 100, sort = "departureTime", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return scheduleService.list(date, routeId, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','DRIVER')")
    public ScheduleDtos.Response get(@PathVariable Long id) {
        return scheduleService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ScheduleDtos.Response create(@Valid @RequestBody ScheduleDtos.Request request, @AuthenticationPrincipal User user) {
        return scheduleService.create(request, user.getEmail());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ScheduleDtos.Response update(
            @PathVariable Long id,
            @Valid @RequestBody ScheduleDtos.Request request,
            @AuthenticationPrincipal User user
    ) {
        return scheduleService.update(id, request, user.getEmail());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal User user) {
        scheduleService.delete(id, user.getEmail());
        return ResponseEntity.noContent().build();
    }
}
