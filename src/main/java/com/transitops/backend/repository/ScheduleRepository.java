package com.transitops.backend.repository;

import com.transitops.backend.entity.Schedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    Page<Schedule> findByServiceDate(LocalDate date, Pageable pageable);
    Page<Schedule> findByRouteId(Long routeId, Pageable pageable);
    List<Schedule> findByDriverIdAndServiceDate(Long driverId, LocalDate date);
    List<Schedule> findByVehicleIdAndServiceDate(Long vehicleId, LocalDate date);
    long countByDelayStatusIgnoreCase(String delayStatus);
    long countByStatusIgnoreCase(String status);
    long countByServiceDate(LocalDate date);
}
