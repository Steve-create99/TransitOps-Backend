package com.transitops.backend.controller;

import com.transitops.backend.dto.StopDtos;
import com.transitops.backend.dto.common.PageResponse;
import com.transitops.backend.entity.User;
import com.transitops.backend.service.StopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stops")
@RequiredArgsConstructor
public class StopController {

    private final StopService stopService;

    @GetMapping
    public PageResponse<StopDtos.Response> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String zone,
            @PageableDefault(size = 50, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return stopService.list(search, zone, pageable);
    }

    @GetMapping("/{id}")
    public StopDtos.Response get(@PathVariable Long id) {
        return stopService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<StopDtos.Response> create(
            @Valid @RequestBody StopDtos.Request request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(stopService.create(request, user.getEmail()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public StopDtos.Response update(
            @PathVariable Long id,
            @Valid @RequestBody StopDtos.Request request,
            @AuthenticationPrincipal User user
    ) {
        return stopService.update(id, request, user.getEmail());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal User user) {
        stopService.delete(id, user.getEmail());
        return ResponseEntity.noContent().build();
    }
}
