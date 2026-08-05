package com.transitops.backend.service;

import com.transitops.backend.dto.DriverMeDtos;
import com.transitops.backend.dto.VehicleDtos;
import com.transitops.backend.entity.*;
import com.transitops.backend.exception.ApiException;
import com.transitops.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class DriverMeService {

    private static final ZoneId ZONE = ZoneId.of("Africa/Accra");
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("hh:mm a", Locale.US).withZone(ZONE);

    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final ScheduleRepository scheduleRepository;
    private final AttendanceRepository attendanceRepository;
    private final IncidentRepository incidentRepository;
    private final VehicleService vehicleService;
    private final AuditService auditService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /** In-memory trip status for the mobile companion (keyed by driver id). */
    private final Map<Long, TripSession> tripSessions = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public DriverMeDtos.ProfileResponse profile(User user) {
        Driver driver = requireDriver(user);
        Vehicle vehicle = vehicleRepository.findFirstByAssignedDriverId(driver.getId()).orElse(null);
        RouteEntity route = driver.getAssignedRoute();

        return DriverMeDtos.ProfileResponse.builder()
                .id(driver.getId())
                .userId(user.getId())
                .firstName(driver.getFirstName())
                .lastName(driver.getLastName())
                .email(driver.getEmail() != null ? driver.getEmail() : user.getEmail())
                .phone(driver.getPhone())
                .photoUrl(driver.getPhotoUrl())
                .licenseNumber(driver.getLicenseNumber())
                .licenseExpiry(driver.getLicenseExpiry())
                .employmentStatus(driver.getEmploymentStatus())
                .availability(driver.getAvailability())
                .employeeId("EMP-" + driver.getId())
                .assignedRouteId(route != null ? route.getId() : null)
                .assignedRouteCode(route != null ? route.getCode() : null)
                .assignedRouteName(route != null ? route.getName() : null)
                .assignedVehicleId(vehicle != null ? vehicle.getId() : null)
                .assignedVehicle(vehicle != null
                        ? vehicle.getRegistrationNumber() + " (" + nullSafe(vehicle.getMake()) + " " + nullSafe(vehicle.getModel()) + ")"
                        : null)
                .assignedDepot("KNUST Main Terminal Depot")
                .build();
    }

    @Transactional
    public DriverMeDtos.ProfileResponse updateProfile(User user, DriverMeDtos.ProfileUpdateRequest req) {
        Driver driver = requireDriver(user);
        if (req.getPhone() != null && !req.getPhone().isBlank()) {
            driver.setPhone(req.getPhone().trim());
        }
        if (req.getPhotoUrl() != null) {
            driver.setPhotoUrl(req.getPhotoUrl());
        }
        auditService.log(user.getEmail(), "UPDATE", "Driver", String.valueOf(driver.getId()), "Self profile update");
        return profile(user);
    }

    @Transactional(readOnly = true)
    public DriverMeDtos.ShiftResponse currentShift(User user) {
        Driver driver = requireDriver(user);
        Vehicle vehicle = vehicleRepository.findFirstByAssignedDriverId(driver.getId()).orElse(null);
        RouteEntity route = driver.getAssignedRoute();
        List<Schedule> today = scheduleRepository.findByDriverIdAndServiceDate(driver.getId(), LocalDate.now(ZONE));
        Schedule schedule = today.isEmpty() ? null : today.get(0);

        String start = schedule != null && schedule.getDepartureTime() != null
                ? schedule.getDepartureTime().format(DateTimeFormatter.ofPattern("hh:mm a", Locale.US))
                : "06:00 AM";
        String end = schedule != null && schedule.getArrivalTime() != null
                ? schedule.getArrivalTime().format(DateTimeFormatter.ofPattern("hh:mm a", Locale.US))
                : "02:00 PM";

        String status = "SCHEDULED";
        if (schedule != null && schedule.getStatus() != null) {
            status = switch (schedule.getStatus().toUpperCase(Locale.ROOT)) {
                case "IN_PROGRESS", "RUNNING" -> "IN_PROGRESS";
                case "COMPLETED", "ENDED" -> "COMPLETED";
                default -> "SCHEDULED";
            };
        } else if ("ON_SHIFT".equalsIgnoreCase(driver.getAvailability())) {
            status = "IN_PROGRESS";
        }

        List<String> stops = buildStopList(route);

        return DriverMeDtos.ShiftResponse.builder()
                .shiftId(schedule != null ? "SH-" + schedule.getId() : "SH-DRV-" + driver.getId())
                .shiftName(route != null ? route.getName() + " Shift" : "Assigned Shift")
                .startTime(start)
                .endTime(end)
                .busNumber(vehicle != null ? vehicle.getRegistrationNumber() : "Unassigned")
                .routeNumber(route != null ? route.getCode() : "N/A")
                .routeName(route != null ? route.getName() : "No route assigned")
                .status(status)
                .routeId(route != null ? route.getId() : null)
                .vehicleId(vehicle != null ? vehicle.getId() : null)
                .stops(stops)
                .build();
    }

    public DriverMeDtos.AttendanceResponse todayAttendance(User user) {
        Driver driver = requireDriver(user);
        return attendanceRepository.findByDriverIdAndDate(driver.getId(), LocalDate.now(ZONE))
                .map(this::toAttendanceResponse)
                .orElse(null);
    }

    public List<DriverMeDtos.AttendanceResponse> attendanceHistory(User user) {
        Driver driver = requireDriver(user);
        return attendanceRepository.findByDriverIdOrderByDateDesc(driver.getId()).stream()
                .map(this::toAttendanceResponse)
                .toList();
    }

    @Transactional
    public DriverMeDtos.AttendanceResponse checkIn(User user) {
        Driver driver = requireDriver(user);
        LocalDate today = LocalDate.now(ZONE);
        Attendance attendance = attendanceRepository.findByDriverIdAndDate(driver.getId(), today)
                .orElseGet(() -> Attendance.builder().driver(driver).date(today).build());

        if (attendance.getCheckInAt() != null && attendance.getCheckOutAt() == null) {
            throw new ApiException("Already checked in for today", HttpStatus.CONFLICT);
        }

        Instant now = Instant.now();
        attendance.setCheckInAt(now);
        attendance.setCheckOutAt(null);
        attendance.setStatus("ON_DUTY");
        attendance.setNotes("Mobile check-in");
        attendanceRepository.save(attendance);
        driver.setAvailability("ON_SHIFT");
        driverRepository.save(driver);
        auditService.log(user.getEmail(), "CHECK_IN", "Attendance", String.valueOf(attendance.getId()), "Driver check-in");
        return toAttendanceResponse(attendance);
    }

    @Transactional
    public DriverMeDtos.AttendanceResponse checkOut(User user) {
        Driver driver = requireDriver(user);
        LocalDate today = LocalDate.now(ZONE);
        Attendance attendance = attendanceRepository.findByDriverIdAndDate(driver.getId(), today)
                .orElseThrow(() -> new ApiException("No check-in found for today", HttpStatus.BAD_REQUEST));

        if (attendance.getCheckInAt() == null) {
            throw new ApiException("No check-in found for today", HttpStatus.BAD_REQUEST);
        }
        if (attendance.getCheckOutAt() != null) {
            throw new ApiException("Already checked out for today", HttpStatus.CONFLICT);
        }

        attendance.setCheckOutAt(Instant.now());
        attendance.setStatus("PRESENT");
        attendance.setNotes("Mobile check-out");
        attendanceRepository.save(attendance);
        driver.setAvailability("AVAILABLE");
        driverRepository.save(driver);
        tripSessions.remove(driver.getId());
        auditService.log(user.getEmail(), "CHECK_OUT", "Attendance", String.valueOf(attendance.getId()), "Driver check-out");
        return toAttendanceResponse(attendance);
    }

    @Transactional
    public Map<String, Object> pingLocation(User user, DriverMeDtos.LocationRequest req) {
        Driver driver = requireDriver(user);
        Vehicle vehicle = vehicleRepository.findFirstByAssignedDriverId(driver.getId())
                .orElseThrow(() -> new ApiException("No vehicle assigned for GPS updates", HttpStatus.BAD_REQUEST));

        VehicleDtos.GpsPing ping = new VehicleDtos.GpsPing();
        ping.setLatitude(req.getLatitude());
        ping.setLongitude(req.getLongitude());
        vehicleService.gpsPing(vehicle.getId(), ping, user.getEmail());
        return Map.of(
                "success", true,
                "vehicleId", vehicle.getId(),
                "latitude", req.getLatitude(),
                "longitude", req.getLongitude()
        );
    }

    public List<Incident> myIncidents(User user) {
        Driver driver = requireDriver(user);
        return incidentRepository.findByDriverIdOrderByCreatedAtDesc(driver.getId());
    }

    @Transactional
    public Incident reportIncident(User user, DriverMeDtos.IncidentCreateRequest req) {
        Driver driver = requireDriver(user);
        String category = req.getCategory() != null ? req.getCategory() : "OTHER";
        String title = req.getTitle() != null && !req.getTitle().isBlank()
                ? req.getTitle()
                : category.replace('_', ' ') + " report";
        String severity = req.getSeverity() != null ? req.getSeverity() : mapSeverity(category);

        Incident incident = Incident.builder()
                .driver(driver)
                .title(title)
                .description(req.getDescription())
                .category(category)
                .severity(severity)
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .status("OPEN")
                .build();
        incidentRepository.save(incident);
        auditService.log(user.getEmail(), "CREATE", "Incident", String.valueOf(incident.getId()), title);
        String msg = driver.getFirstName() + " " + driver.getLastName() + ": " + title
                + (severity != null ? " (" + severity + ")" : "");
        String priority = "HIGH".equalsIgnoreCase(severity) || "CRITICAL".equalsIgnoreCase(severity) ? "HIGH" : "MEDIUM";
        for (User admin : userRepository.findByRole(Role.ADMIN)) {
            try {
                notificationService.create("New incident", msg, "INCIDENT", priority, admin.getId());
            } catch (Exception ignored) {
                // best-effort
            }
        }
        return incident;
    }

    @Transactional(readOnly = true)
    public DriverMeDtos.TripResponse activeTrip(User user) {
        Driver driver = requireDriver(user);
        DriverMeDtos.ShiftResponse shift = currentShift(user);
        List<String> stops = shift.getStops() != null ? shift.getStops() : List.of();
        TripSession session = tripSessions.computeIfAbsent(driver.getId(), id -> new TripSession());

        String nextStop = null;
        if (!stops.isEmpty()) {
            int idx = Math.min(session.completedStopsCount, stops.size() - 1);
            nextStop = stops.get(idx);
        }

        long elapsed = session.elapsedSeconds;
        if ("STARTED".equals(session.status) || "RESUMED".equals(session.status)) {
            if (session.runningSince != null) {
                elapsed += Duration.between(session.runningSince, Instant.now()).getSeconds();
            }
        }

        return DriverMeDtos.TripResponse.builder()
                .id("TRIP-" + driver.getId())
                .routeId(shift.getRouteId() != null ? String.valueOf(shift.getRouteId()) : null)
                .routeName(shift.getRouteName())
                .routeNumber(shift.getRouteNumber())
                .busNumber(shift.getBusNumber())
                .status(session.status)
                .completedStopsCount(session.completedStopsCount)
                .totalStopsCount(Math.max(stops.size(), 1))
                .nextStopName(nextStop)
                .elapsedSeconds(elapsed)
                .stops(stops)
                .build();
    }

    @Transactional
    public DriverMeDtos.TripResponse updateTripStatus(User user, String status) {
        Driver driver = requireDriver(user);
        TripSession session = tripSessions.computeIfAbsent(driver.getId(), id -> new TripSession());
        String normalized = status == null ? "IDLE" : status.toUpperCase(Locale.ROOT);

        Instant now = Instant.now();
        switch (normalized) {
            case "STARTED" -> {
                accumulate(session, now);
                session.status = "STARTED";
                session.runningSince = now;
                driver.setAvailability("ON_SHIFT");
            }
            case "RESUMED" -> {
                accumulate(session, now);
                session.status = "RESUMED";
                session.runningSince = now;
                driver.setAvailability("ON_SHIFT");
            }
            case "PAUSED" -> {
                accumulate(session, now);
                session.status = "PAUSED";
                session.runningSince = null;
            }
            case "ENDED" -> {
                accumulate(session, now);
                session.status = "ENDED";
                session.runningSince = null;
                driver.setAvailability("AVAILABLE");
            }
            default -> session.status = "IDLE";
        }

        driverRepository.save(driver);
        return activeTrip(user);
    }

    private void accumulate(TripSession session, Instant now) {
        if (session.runningSince != null) {
            session.elapsedSeconds += Duration.between(session.runningSince, now).getSeconds();
            session.runningSince = null;
        }
    }

    private Driver requireDriver(User user) {
        return driverRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException(
                        "No driver profile linked to this account. Ask an admin to link your user to a driver record.",
                        HttpStatus.NOT_FOUND));
    }

    private DriverMeDtos.AttendanceResponse toAttendanceResponse(Attendance a) {
        Double hours = null;
        if (a.getCheckInAt() != null && a.getCheckOutAt() != null) {
            hours = Duration.between(a.getCheckInAt(), a.getCheckOutAt()).toMinutes() / 60.0;
            hours = Math.round(hours * 10.0) / 10.0;
        } else if (a.getCheckInAt() != null && a.getCheckOutAt() == null) {
            hours = Duration.between(a.getCheckInAt(), Instant.now()).toMinutes() / 60.0;
            hours = Math.round(hours * 10.0) / 10.0;
        }

        return DriverMeDtos.AttendanceResponse.builder()
                .id(a.getId())
                .date(a.getDate())
                .checkInTime(a.getCheckInAt() != null ? TIME_FMT.format(a.getCheckInAt()) : null)
                .checkOutTime(a.getCheckOutAt() != null ? TIME_FMT.format(a.getCheckOutAt()) : null)
                .shiftDurationHours(hours)
                .status(a.getStatus())
                .checkInAt(a.getCheckInAt())
                .checkOutAt(a.getCheckOutAt())
                .build();
    }

    private List<String> buildStopList(RouteEntity route) {
        if (route == null) return List.of();
        List<String> stops = new ArrayList<>();
        if (route.getStartStop() != null) stops.add(route.getStartStop());
        if (route.getIntermediateStops() != null) stops.addAll(route.getIntermediateStops());
        if (route.getEndStop() != null && !route.getEndStop().equals(route.getStartStop())) {
            stops.add(route.getEndStop());
        }
        return stops;
    }

    private String mapSeverity(String category) {
        if (category == null) return "LOW";
        return switch (category.toUpperCase(Locale.ROOT)) {
            case "ACCIDENT", "VEHICLE_BREAKDOWN" -> "HIGH";
            case "ROAD_CLOSURE", "TRAFFIC" -> "MEDIUM";
            default -> "LOW";
        };
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private static class TripSession {
        private String status = "IDLE";
        private int completedStopsCount = 0;
        private long elapsedSeconds = 0;
        private Instant runningSince;
    }
}
