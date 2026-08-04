package com.transitops.backend.repository;

import com.transitops.backend.entity.RouteEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RouteRepository extends JpaRepository<RouteEntity, Long> {
    Optional<RouteEntity> findByCode(String code);
    boolean existsByCode(String code);
    Page<RouteEntity> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String name, String code, Pageable pageable);
    Page<RouteEntity> findByStatusIgnoreCase(String status, Pageable pageable);
    long countByStatusIgnoreCase(String status);
}
