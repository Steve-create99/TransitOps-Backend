package com.transitops.backend.controller;

import com.transitops.backend.dto.DriverDtos;
import com.transitops.backend.dto.common.PageResponse;
import com.transitops.backend.entity.Attendance;
import com.transitops.backend.entity.Incident;
import com.transitops.backend.entity.User;
import com.transitops.backend.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @GetMapping
    public PageResponse<DriverDtos.Response> list(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        return driverService.list(search, pageable);
    }

    @GetMapping("/{id}")
    public DriverDtos.Response get(@PathVariable Long id) {
        return driverService.get(id);
    }

    @GetMapping("/{id}/profile")
    public Map<String, Object> profile(@PathVariable Long id) {
        return driverService.profileBundle(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public DriverDtos.Response create(@Valid @RequestBody DriverDtos.Request request, @AuthenticationPrincipal User user) {
        return driverService.create(request, user.getEmail());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public DriverDtos.Response update(
            @PathVariable Long id,
            @Valid @RequestBody DriverDtos.Request request,
            @AuthenticationPrincipal User user
    ) {
        return driverService.update(id, request, user.getEmail());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal User user) {
        driverService.delete(id, user.getEmail());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/incidents")
    public List<Incident> incidents(@PathVariable Long id) {
        return driverService.incidents(id);
    }

    @PostMapping("/{id}/incidents")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','DRIVER')")
    public Incident addIncident(
            @PathVariable Long id,
            @Valid @RequestBody DriverDtos.IncidentRequest request,
            @AuthenticationPrincipal User user
    ) {
        return driverService.addIncident(id, request, user.getEmail());
    }

    @GetMapping("/{id}/attendance")
    public List<Attendance> attendance(@PathVariable Long id) {
        return driverService.attendance(id);
    }

    @PostMapping("/{id}/attendance")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public Attendance addAttendance(
            @PathVariable Long id,
            @RequestBody DriverDtos.AttendanceRequest request,
            @AuthenticationPrincipal User user
    ) {
        return driverService.addAttendance(id, request, user.getEmail());
    }
}
