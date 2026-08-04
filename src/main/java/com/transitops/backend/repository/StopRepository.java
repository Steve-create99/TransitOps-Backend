package com.transitops.backend.repository;

import com.transitops.backend.entity.Stop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StopRepository extends JpaRepository<Stop, Long> {
    Optional<Stop> findByName(String name);
    boolean existsByName(String name);
    Page<Stop> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<Stop> findByZoneIgnoreCase(String zone, Pageable pageable);
    Page<Stop> findByNameContainingIgnoreCaseAndZoneIgnoreCase(String name, String zone, Pageable pageable);
    List<Stop> findByZoneIgnoreCase(String zone);
}
