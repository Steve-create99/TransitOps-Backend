package com.transitops.backend.repository;

import com.transitops.backend.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findByRegistrationNumber(String registrationNumber);
    boolean existsByRegistrationNumber(String registrationNumber);
    Page<Vehicle> findByRegistrationNumberContainingIgnoreCase(String reg, Pageable pageable);
    long countByStatusIgnoreCase(String status);
    List<Vehicle> findByStatusIgnoreCase(String status);
    Optional<Vehicle> findFirstByAssignedDriverId(Long driverId);
}
