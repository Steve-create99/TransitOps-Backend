package com.transitops.backend.controller;

import com.transitops.backend.dto.DriverMeDtos;
import com.transitops.backend.entity.Incident;
import com.transitops.backend.entity.User;
import com.transitops.backend.service.DriverMeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/drivers/me")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('DRIVER','ADMIN','DISPATCHER')")
public class DriverMeController {

    private final DriverMeService driverMeService;

    @GetMapping
    public DriverMeDtos.ProfileResponse profile(@AuthenticationPrincipal User user) {
        return driverMeService.profile(user);
    }

    @PutMapping
    public DriverMeDtos.ProfileResponse update(
            @AuthenticationPrincipal User user,
            @RequestBody DriverMeDtos.ProfileUpdateRequest request
    ) {
        return driverMeService.updateProfile(user, request);
    }

    @GetMapping("/shift")
    public DriverMeDtos.ShiftResponse shift(@AuthenticationPrincipal User user) {
        return driverMeService.currentShift(user);
    }

    @GetMapping("/attendance/today")
    public DriverMeDtos.AttendanceResponse today(@AuthenticationPrincipal User user) {
        return driverMeService.todayAttendance(user);
    }

    @GetMapping("/attendance")
    public List<DriverMeDtos.AttendanceResponse> attendance(@AuthenticationPrincipal User user) {
        return driverMeService.attendanceHistory(user);
    }

    @PostMapping("/attendance/check-in")
    public DriverMeDtos.AttendanceResponse checkIn(@AuthenticationPrincipal User user) {
        return driverMeService.checkIn(user);
    }

    @PostMapping("/attendance/check-out")
    public DriverMeDtos.AttendanceResponse checkOut(@AuthenticationPrincipal User user) {
        return driverMeService.checkOut(user);
    }

    @PostMapping("/location")
    public Map<String, Object> location(
            @AuthenticationPrincipal User user,
            @RequestBody DriverMeDtos.LocationRequest request
    ) {
        return driverMeService.pingLocation(user, request);
    }

    @GetMapping("/incidents")
    public List<Incident> incidents(@AuthenticationPrincipal User user) {
        return driverMeService.myIncidents(user);
    }

    @PostMapping("/incidents")
    public Incident createIncident(
            @AuthenticationPrincipal User user,
            @RequestBody DriverMeDtos.IncidentCreateRequest request
    ) {
        return driverMeService.reportIncident(user, request);
    }

    @GetMapping("/trips/active")
    public DriverMeDtos.TripResponse activeTrip(@AuthenticationPrincipal User user) {
        return driverMeService.activeTrip(user);
    }

    @PostMapping("/trips/status")
    public DriverMeDtos.TripResponse tripStatus(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body
    ) {
        return driverMeService.updateTripStatus(user, body.get("status"));
    }
}
