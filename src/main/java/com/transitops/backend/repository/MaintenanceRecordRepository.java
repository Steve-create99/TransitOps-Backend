package com.transitops.backend.repository;

import com.transitops.backend.entity.MaintenanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {
    List<MaintenanceRecord> findByVehicleIdOrderByServiceDateDesc(Long vehicleId);
}
