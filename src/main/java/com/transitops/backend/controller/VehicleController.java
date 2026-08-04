package com.transitops.backend.controller;

import com.transitops.backend.dto.VehicleDtos;
import com.transitops.backend.dto.common.PageResponse;
import com.transitops.backend.entity.MaintenanceRecord;
import com.transitops.backend.entity.User;
import com.transitops.backend.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @GetMapping
    public PageResponse<VehicleDtos.Response> list(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        return vehicleService.list(search, pageable);
    }

    @GetMapping("/locations")
    public List<VehicleDtos.Response> locations() {
        return vehicleService.locations();
    }

    @GetMapping("/{id}")
    public VehicleDtos.Response get(@PathVariable Long id) {
        return vehicleService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public VehicleDtos.Response create(@Valid @RequestBody VehicleDtos.Request request, @AuthenticationPrincipal User user) {
        return vehicleService.create(request, user.getEmail());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public VehicleDtos.Response update(
            @PathVariable Long id,
            @Valid @RequestBody VehicleDtos.Request request,
            @AuthenticationPrincipal User user
    ) {
        return vehicleService.update(id, request, user.getEmail());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal User user) {
        vehicleService.delete(id, user.getEmail());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/gps")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','DRIVER')")
    public VehicleDtos.Response gpsPing(
            @PathVariable Long id,
            @RequestBody VehicleDtos.GpsPing ping,
            @AuthenticationPrincipal User user
    ) {
        return vehicleService.gpsPing(id, ping, user.getEmail());
    }

    @GetMapping("/{id}/maintenance")
    public List<MaintenanceRecord> maintenance(@PathVariable Long id) {
        return vehicleService.maintenance(id);
    }

    @PostMapping("/{id}/maintenance")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public MaintenanceRecord addMaintenance(
            @PathVariable Long id,
            @RequestBody VehicleDtos.MaintenanceRequest request,
            @AuthenticationPrincipal User user
    ) {
        return vehicleService.addMaintenance(id, request, user.getEmail());
    }
}
