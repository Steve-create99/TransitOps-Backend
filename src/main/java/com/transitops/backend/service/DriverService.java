package com.transitops.backend.service;

import com.transitops.backend.dto.DriverDtos;
import com.transitops.backend.dto.common.PageResponse;
import com.transitops.backend.entity.*;
import com.transitops.backend.exception.ApiException;
import com.transitops.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;
    private final RouteRepository routeRepository;
    private final UserRepository userRepository;
    private final IncidentRepository incidentRepository;
    private final AttendanceRepository attendanceRepository;
    private final AuditService auditService;

    public PageResponse<DriverDtos.Response> list(String search, Pageable pageable) {
        Page<Driver> page = (search != null && !search.isBlank())
                ? driverRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(search, search, pageable)
                : driverRepository.findAll(pageable);
        return PageResponse.from(page.map(DriverDtos.Response::from));
    }

    public DriverDtos.Response get(Long id) {
        return DriverDtos.Response.from(find(id));
    }

    @Transactional
    public DriverDtos.Response create(DriverDtos.Request req, String actor) {
        Driver d = map(new Driver(), req);
        driverRepository.save(d);
        auditService.log(actor, "CREATE", "Driver", String.valueOf(d.getId()), d.getFirstName());
        return DriverDtos.Response.from(d);
    }

    @Transactional
    public DriverDtos.Response update(Long id, DriverDtos.Request req, String actor) {
        Driver d = map(find(id), req);
        auditService.log(actor, "UPDATE", "Driver", String.valueOf(id), d.getFirstName());
        return DriverDtos.Response.from(d);
    }

    @Transactional
    public void delete(Long id, String actor) {
        Driver d = find(id);
        driverRepository.delete(d);
        auditService.log(actor, "DELETE", "Driver", String.valueOf(id), d.getFirstName());
    }

    public List<Incident> incidents(Long driverId) {
        find(driverId);
        return incidentRepository.findByDriverIdOrderByCreatedAtDesc(driverId);
    }

    @Transactional
    public Incident addIncident(Long driverId, DriverDtos.IncidentRequest req, String actor) {
        Driver d = find(driverId);
        Incident i = Incident.builder()
                .driver(d)
                .title(req.getTitle())
                .description(req.getDescription())
                .severity(req.getSeverity() != null ? req.getSeverity() : "LOW")
                .build();
        incidentRepository.save(i);
        auditService.log(actor, "CREATE", "Incident", String.valueOf(i.getId()), req.getTitle());
        return i;
    }

    public List<Attendance> attendance(Long driverId) {
        find(driverId);
        return attendanceRepository.findByDriverIdOrderByDateDesc(driverId);
    }

    @Transactional
    public Attendance addAttendance(Long driverId, DriverDtos.AttendanceRequest req, String actor) {
        Driver d = find(driverId);
        Attendance a = Attendance.builder()
                .driver(d)
                .date(req.getDate() != null ? req.getDate() : LocalDate.now())
                .status(req.getStatus() != null ? req.getStatus() : "PRESENT")
                .notes(req.getNotes())
                .build();
        attendanceRepository.save(a);
        auditService.log(actor, "CREATE", "Attendance", String.valueOf(a.getId()), a.getStatus());
        return a;
    }

    public Map<String, Object> profileBundle(Long id) {
        Driver d = find(id);
        return Map.of(
                "driver", DriverDtos.Response.from(d),
                "incidents", incidents(id),
                "attendance", attendance(id)
        );
    }

    public Driver find(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new ApiException("Driver not found", HttpStatus.NOT_FOUND));
    }

    private Driver map(Driver d, DriverDtos.Request req) {
        d.setFirstName(req.getFirstName());
        d.setLastName(req.getLastName());
        d.setEmail(req.getEmail());
        d.setPhone(req.getPhone());
        d.setPhotoUrl(req.getPhotoUrl());
        d.setLicenseNumber(req.getLicenseNumber());
        d.setLicenseExpiry(req.getLicenseExpiry());
        d.setEmploymentStatus(req.getEmploymentStatus() != null ? req.getEmploymentStatus() : "ACTIVE");
        d.setAvailability(req.getAvailability() != null ? req.getAvailability() : "AVAILABLE");
        if (req.getAssignedRouteId() != null) {
            d.setAssignedRoute(routeRepository.findById(req.getAssignedRouteId())
                    .orElseThrow(() -> new ApiException("Route not found", HttpStatus.NOT_FOUND)));
        } else {
            d.setAssignedRoute(null);
        }
        if (req.getUserId() != null) {
            d.setUser(userRepository.findById(req.getUserId())
                    .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND)));
        }
        return d;
    }
}
