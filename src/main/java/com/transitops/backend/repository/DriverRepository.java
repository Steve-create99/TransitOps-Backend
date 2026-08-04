package com.transitops.backend.repository;

import com.transitops.backend.entity.Driver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {
    Optional<Driver> findByUserId(Long userId);
    Optional<Driver> findByLicenseNumber(String licenseNumber);
    Page<Driver> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(String first, String last, Pageable pageable);
    long countByEmploymentStatusIgnoreCase(String status);
}
