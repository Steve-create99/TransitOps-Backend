package com.transitops.backend.repository;

import com.transitops.backend.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByDriverIdOrderByCreatedAtDesc(Long driverId);
}
