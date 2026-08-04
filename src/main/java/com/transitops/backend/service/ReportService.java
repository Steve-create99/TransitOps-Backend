package com.transitops.backend.service;

import com.transitops.backend.entity.Stop;
import com.transitops.backend.entity.TripMetric;
import com.transitops.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TripMetricRepository tripMetricRepository;
    private final RouteRepository routeRepository;
    private final StopRepository stopRepository;
    private final ScheduleRepository scheduleRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    public Map<String, Object> report(String period) {
        LocalDate end = LocalDate.now();
        LocalDate start = switch (period == null ? "weekly" : period.toLowerCase()) {
            case "daily" -> end;
            case "monthly" -> end.minusDays(29);
            default -> end.minusDays(6);
        };

        List<TripMetric> metrics = tripMetricRepository.findByMetricDateBetween(start, end);
        long ridership = metrics.stream().mapToLong(m -> m.getPassengers() != null ? m.getPassengers() : 0).sum();
        long completed = metrics.stream().mapToLong(m -> m.getCompletedTrips() != null ? m.getCompletedTrips() : 0).sum();
        long delayed = metrics.stream().mapToLong(m -> m.getDelayedTrips() != null ? m.getDelayedTrips() : 0).sum();
        double onTime = metrics.stream().mapToDouble(m -> m.getOnTimePercentage() != null ? m.getOnTimePercentage() : 100).average().orElse(100);
        double avgSpeed = metrics.stream().mapToDouble(m -> m.getAverageSpeed() != null ? m.getAverageSpeed() : 0).average().orElse(0);

        Map<Long, Long> routePassengers = metrics.stream()
                .filter(m -> m.getRoute() != null)
                .collect(Collectors.groupingBy(m -> m.getRoute().getId(),
                        Collectors.summingLong(m -> m.getPassengers() != null ? m.getPassengers() : 0)));

        List<Map<String, Object>> busiestRoutes = routePassengers.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    var route = routeRepository.findById(e.getKey()).orElse(null);
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("routeId", e.getKey());
                    row.put("routeName", route != null ? route.getName() : "Unknown");
                    row.put("routeCode", route != null ? route.getCode() : "");
                    row.put("passengers", e.getValue());
                    return row;
                }).toList();

        List<Map<String, Object>> busiestStops = stopRepository.findAll().stream()
                .sorted(Comparator.comparing((Stop s) -> s.getAverageRiders() != null ? s.getAverageRiders() : 0).reversed())
                .limit(10)
                .map(s -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("stopId", s.getId());
                    row.put("name", s.getName());
                    row.put("zone", s.getZone());
                    row.put("riders", s.getAverageRiders());
                    return row;
                }).toList();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("period", period != null ? period : "weekly");
        summary.put("startDate", start.toString());
        summary.put("endDate", end.toString());
        summary.put("ridership", ridership);
        summary.put("completedTrips", completed);
        summary.put("delays", delayed);
        summary.put("onTimePercentage", Math.round(onTime * 10.0) / 10.0);
        summary.put("averageSpeed", Math.round(avgSpeed * 10.0) / 10.0);
        summary.put("activeDrivers", driverRepository.countByEmploymentStatusIgnoreCase("ACTIVE"));
        summary.put("vehicleUtilization", vehicleRepository.countByStatusIgnoreCase("ACTIVE"));
        summary.put("scheduledToday", scheduleRepository.countByServiceDate(LocalDate.now()));
        summary.put("busiestRoutes", busiestRoutes);
        summary.put("busiestStops", busiestStops);
        summary.put("dailySeries", buildDailySeries(metrics, start, end));
        return summary;
    }

    public String toCsv(String period) {
        Map<String, Object> data = report(period);
        StringBuilder sb = new StringBuilder("metric,value\n");
        data.forEach((k, v) -> {
            if (!(v instanceof Collection) && !(v instanceof Map)) {
                sb.append(k).append(',').append(v).append('\n');
            }
        });
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> routes = (List<Map<String, Object>>) data.get("busiestRoutes");
        sb.append("\nrouteCode,routeName,passengers\n");
        for (Map<String, Object> r : routes) {
            sb.append(r.get("routeCode")).append(',')
                    .append('"').append(r.get("routeName")).append('"').append(',')
                    .append(r.get("passengers")).append('\n');
        }
        return sb.toString();
    }

    private List<Map<String, Object>> buildDailySeries(List<TripMetric> metrics, LocalDate start, LocalDate end) {
        Map<LocalDate, Long> byDay = metrics.stream()
                .collect(Collectors.groupingBy(TripMetric::getMetricDate,
                        Collectors.summingLong(m -> m.getPassengers() != null ? m.getPassengers() : 0)));
        List<Map<String, Object>> series = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", d.toString());
            point.put("passengers", byDay.getOrDefault(d, 0L));
            series.add(point);
        }
        return series;
    }
}
