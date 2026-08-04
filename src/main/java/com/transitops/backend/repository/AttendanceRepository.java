package com.transitops.backend.repository;

import com.transitops.backend.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByDriverIdOrderByDateDesc(Long driverId);
    Optional<Attendance> findByDriverIdAndDate(Long driverId, LocalDate date);
}
