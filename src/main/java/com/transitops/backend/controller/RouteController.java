package com.transitops.backend.controller;

import com.transitops.backend.dto.RouteDtos;
import com.transitops.backend.dto.common.PageResponse;
import com.transitops.backend.entity.User;
import com.transitops.backend.service.RouteService;
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
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @GetMapping
    public PageResponse<RouteDtos.Response> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 50, sort = "code", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return routeService.list(search, status, pageable);
    }

    @GetMapping("/{id}")
    public RouteDtos.Response get(@PathVariable Long id) {
        return routeService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public RouteDtos.Response create(@Valid @RequestBody RouteDtos.Request request, @AuthenticationPrincipal User user) {
        return routeService.create(request, user.getEmail());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public RouteDtos.Response update(
            @PathVariable Long id,
            @Valid @RequestBody RouteDtos.Request request,
            @AuthenticationPrincipal User user
    ) {
        return routeService.update(id, request, user.getEmail());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal User user) {
        routeService.delete(id, user.getEmail());
        return ResponseEntity.noContent().build();
    }
}
