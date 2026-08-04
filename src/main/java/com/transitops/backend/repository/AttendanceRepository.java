package com.transitops.backend.repository;

import com.transitops.backend.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByDriverIdOrderByDateDesc(Long driverId);
}
