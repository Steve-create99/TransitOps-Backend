package com.transitops.backend.service;

import com.transitops.backend.entity.RouteEntity;
import com.transitops.backend.entity.TripMetric;
import com.transitops.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TripMetricRepository tripMetricRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final ScheduleRepository scheduleRepository;
    private final NotificationRepository notificationRepository;
    private final RouteRepository routeRepository;
    private final StopRepository stopRepository;

    public Map<String, Object> kpis() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);

        long passengersToday = tripMetricRepository.sumPassengersForDate(today);
        long activeBuses = vehicleRepository.countByStatusIgnoreCase("ACTIVE");
        long delayedBuses = scheduleRepository.countByDelayStatusIgnoreCase("DELAYED");
        long completedTrips = tripMetricRepository.sumCompletedTrips(today);
        Double avgSpeed = Optional.ofNullable(tripMetricRepository.averageSpeed(weekStart, today)).orElse(0.0);
        Double onTime = Optional.ofNullable(tripMetricRepository.averageOnTime(weekStart, today)).orElse(100.0);
        long activeDrivers = driverRepository.countByEmploymentStatusIgnoreCase("ACTIVE");
        long availableVehicles = vehicleRepository.countByStatusIgnoreCase("AVAILABLE")
                + vehicleRepository.countByStatusIgnoreCase("ACTIVE");
        long activeAlerts = notificationRepository.count();

        // If no metrics yet, derive sensible zeros from fleet state rather than inventing numbers
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("passengersToday", passengersToday);
        result.put("activeBuses", activeBuses);
        result.put("delayedBuses", delayedBuses);
        result.put("completedTrips", completedTrips);
        result.put("averageSpeed", Math.round(avgSpeed * 10.0) / 10.0);
        result.put("onTimePercentage", Math.round(onTime * 10.0) / 10.0);
        result.put("activeAlerts", activeAlerts);
        result.put("activeDrivers", activeDrivers);
        result.put("availableVehicles", availableVehicles);
        result.put("activeRoutes", routeRepository.countByStatusIgnoreCase("Active"));
        result.put("totalStops", stopRepository.count());
        return result;
    }

    public Map<String, Object> charts() {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        List<TripMetric> metrics = tripMetricRepository.findByMetricDateBetween(start, end);

        Map<LocalDate, Long> byDay = metrics.stream()
                .collect(Collectors.groupingBy(TripMetric::getMetricDate,
                        Collectors.summingLong(m -> m.getPassengers() != null ? m.getPassengers() : 0)));

        List<Map<String, Object>> passengerVolume = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", d.toString());
            point.put("day", d.getDayOfWeek().name().substring(0, 3));
            point.put("passengers", byDay.getOrDefault(d, 0L));
            passengerVolume.add(point);
        }

        List<Map<String, Object>> routeStatus = routeRepository.findAll().stream().map(r -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", r.getId());
            row.put("code", r.getCode());
            row.put("name", r.getName());
            row.put("status", r.getStatus());
            row.put("color", r.getColor());
            row.put("buses", r.getBusCount());
            return row;
        }).toList();

        return Map.of(
                "passengerVolume", passengerVolume,
                "routeStatus", routeStatus
        );
    }
}
