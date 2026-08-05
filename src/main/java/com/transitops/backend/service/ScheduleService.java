package com.transitops.backend.service;

import com.transitops.backend.dto.ScheduleDtos;
import com.transitops.backend.dto.common.PageResponse;
import com.transitops.backend.entity.Driver;
import com.transitops.backend.entity.Role;
import com.transitops.backend.entity.Schedule;
import com.transitops.backend.entity.User;
import com.transitops.backend.entity.Vehicle;
import com.transitops.backend.exception.ApiException;
import com.transitops.backend.repository.DriverRepository;
import com.transitops.backend.repository.ScheduleRepository;
import com.transitops.backend.repository.UserRepository;
import com.transitops.backend.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final RouteService routeService;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public PageResponse<ScheduleDtos.Response> list(LocalDate date, Long routeId, Pageable pageable) {
        Page<Schedule> page;
        if (date != null) {
            page = scheduleRepository.findByServiceDate(date, pageable);
        } else if (routeId != null) {
            page = scheduleRepository.findByRouteId(routeId, pageable);
        } else {
            page = scheduleRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(ScheduleDtos.Response::from));
    }

    public ScheduleDtos.Response get(Long id) {
        return ScheduleDtos.Response.from(find(id));
    }

    @Transactional
    public ScheduleDtos.Response create(ScheduleDtos.Request req, String actor) {
        Schedule s = map(new Schedule(), req);
        detectConflicts(s);
        scheduleRepository.save(s);
        auditService.log(actor, "CREATE", "Schedule", String.valueOf(s.getId()), "Created");
        return ScheduleDtos.Response.from(s);
    }

    @Transactional
    public ScheduleDtos.Response update(Long id, ScheduleDtos.Request req, String actor) {
        Schedule s = map(find(id), req);
        detectConflicts(s);
        auditService.log(actor, "UPDATE", "Schedule", String.valueOf(id), "Updated");
        if ("DELAYED".equalsIgnoreCase(s.getDelayStatus()) || "DELAYED".equalsIgnoreCase(s.getStatus())) {
            String routeCode = s.getRoute() != null ? s.getRoute().getCode() : "route";
            String msg = "Schedule for " + routeCode + " marked delayed"
                    + (s.getDelayMinutes() > 0 ? " by " + s.getDelayMinutes() + " min" : "") + ".";
            for (User admin : userRepository.findByRole(Role.ADMIN)) {
                try {
                    notificationService.create("Route delay", msg, "DELAY", "HIGH", admin.getId());
                } catch (Exception ignored) {
                    // best-effort
                }
            }
        }
        return ScheduleDtos.Response.from(s);
    }

    @Transactional
    public void delete(Long id, String actor) {
        Schedule s = find(id);
        scheduleRepository.delete(s);
        auditService.log(actor, "DELETE", "Schedule", String.valueOf(id), "Deleted");
    }

    private void detectConflicts(Schedule s) {
        if (s.getServiceDate() == null) return;
        if (s.getDriver() != null) {
            List<Schedule> driverSchedules = scheduleRepository.findByDriverIdAndServiceDate(
                    s.getDriver().getId(), s.getServiceDate());
            boolean conflict = driverSchedules.stream()
                    .anyMatch(existing -> !existing.getId().equals(s.getId())
                            && timesOverlap(existing, s));
            if (conflict) {
                throw new ApiException("Driver schedule conflict detected", HttpStatus.CONFLICT);
            }
        }
        if (s.getVehicle() != null) {
            List<Schedule> vehicleSchedules = scheduleRepository.findByVehicleIdAndServiceDate(
                    s.getVehicle().getId(), s.getServiceDate());
            boolean conflict = vehicleSchedules.stream()
                    .anyMatch(existing -> !existing.getId().equals(s.getId())
                            && timesOverlap(existing, s));
            if (conflict) {
                throw new ApiException("Vehicle schedule conflict detected", HttpStatus.CONFLICT);
            }
        }
    }

    private boolean timesOverlap(Schedule a, Schedule b) {
        if (a.getDepartureTime() == null || b.getDepartureTime() == null
                || a.getArrivalTime() == null || b.getArrivalTime() == null) {
            return false;
        }
        return !a.getArrivalTime().isBefore(b.getDepartureTime())
                && !b.getArrivalTime().isBefore(a.getDepartureTime());
    }

    private Schedule find(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new ApiException("Schedule not found", HttpStatus.NOT_FOUND));
    }

    private Schedule map(Schedule s, ScheduleDtos.Request req) {
        s.setRoute(routeService.find(req.getRouteId()));
        s.setServiceDate(req.getServiceDate());
        s.setDepartureTime(req.getDepartureTime());
        s.setArrivalTime(req.getArrivalTime());
        s.setWeekdays(req.getWeekdays() == null || req.getWeekdays());
        s.setWeekends(Boolean.TRUE.equals(req.getWeekends()));
        s.setHolidays(Boolean.TRUE.equals(req.getHolidays()));
        s.setDelayStatus(req.getDelayStatus() != null ? req.getDelayStatus() : "ON_TIME");
        s.setDelayMinutes(req.getDelayMinutes() != null ? req.getDelayMinutes() : 0);
        s.setStatus(req.getStatus() != null ? req.getStatus() : "SCHEDULED");
        if (req.getDriverId() != null) {
            Driver d = driverRepository.findById(req.getDriverId())
                    .orElseThrow(() -> new ApiException("Driver not found", HttpStatus.NOT_FOUND));
            s.setDriver(d);
        } else {
            s.setDriver(null);
        }
        if (req.getVehicleId() != null) {
            Vehicle v = vehicleRepository.findById(req.getVehicleId())
                    .orElseThrow(() -> new ApiException("Vehicle not found", HttpStatus.NOT_FOUND));
            s.setVehicle(v);
        } else {
            s.setVehicle(null);
        }
        return s;
    }
}
