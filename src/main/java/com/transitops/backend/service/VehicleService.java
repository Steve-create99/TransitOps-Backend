package com.transitops.backend.service;

import com.transitops.backend.dto.VehicleDtos;
import com.transitops.backend.dto.common.PageResponse;
import com.transitops.backend.entity.MaintenanceRecord;
import com.transitops.backend.entity.Vehicle;
import com.transitops.backend.exception.ApiException;
import com.transitops.backend.repository.DriverRepository;
import com.transitops.backend.repository.MaintenanceRecordRepository;
import com.transitops.backend.repository.RouteRepository;
import com.transitops.backend.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final RouteRepository routeRepository;
    private final DriverRepository driverRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final AuditService auditService;

    public PageResponse<VehicleDtos.Response> list(String search, Pageable pageable) {
        Page<Vehicle> page = (search != null && !search.isBlank())
                ? vehicleRepository.findByRegistrationNumberContainingIgnoreCase(search.trim(), pageable)
                : vehicleRepository.findAll(pageable);
        return PageResponse.from(page.map(VehicleDtos.Response::from));
    }

    public VehicleDtos.Response get(Long id) {
        return VehicleDtos.Response.from(find(id));
    }

    @Transactional
    public VehicleDtos.Response create(VehicleDtos.Request req, String actor) {
        if (vehicleRepository.existsByRegistrationNumber(req.getRegistrationNumber())) {
            throw new ApiException("Registration already exists", HttpStatus.CONFLICT);
        }
        Vehicle v = map(new Vehicle(), req);
        vehicleRepository.save(v);
        auditService.log(actor, "CREATE", "Vehicle", String.valueOf(v.getId()), v.getRegistrationNumber());
        return VehicleDtos.Response.from(v);
    }

    @Transactional
    public VehicleDtos.Response update(Long id, VehicleDtos.Request req, String actor) {
        Vehicle v = find(id);
        if (!v.getRegistrationNumber().equalsIgnoreCase(req.getRegistrationNumber())
                && vehicleRepository.existsByRegistrationNumber(req.getRegistrationNumber())) {
            throw new ApiException("Registration already exists", HttpStatus.CONFLICT);
        }
        map(v, req);
        auditService.log(actor, "UPDATE", "Vehicle", String.valueOf(id), v.getRegistrationNumber());
        return VehicleDtos.Response.from(v);
    }

    @Transactional
    public void delete(Long id, String actor) {
        Vehicle v = find(id);
        vehicleRepository.delete(v);
        auditService.log(actor, "DELETE", "Vehicle", String.valueOf(id), v.getRegistrationNumber());
    }

    public List<VehicleDtos.Response> locations() {
        return vehicleRepository.findAll().stream().map(VehicleDtos.Response::from).toList();
    }

    @Transactional
    public VehicleDtos.Response gpsPing(Long id, VehicleDtos.GpsPing ping, String actor) {
        Vehicle v = find(id);
        v.setLatitude(ping.getLatitude());
        v.setLongitude(ping.getLongitude());
        if (ping.getFuelLevel() != null) v.setFuelLevel(ping.getFuelLevel());
        v.setGpsStatus("ONLINE");
        v.setLastGpsAt(Instant.now());
        auditService.log(actor, "GPS_PING", "Vehicle", String.valueOf(id), "Location update");
        return VehicleDtos.Response.from(v);
    }

    public List<MaintenanceRecord> maintenance(Long id) {
        find(id);
        return maintenanceRecordRepository.findByVehicleIdOrderByServiceDateDesc(id);
    }

    @Transactional
    public MaintenanceRecord addMaintenance(Long id, VehicleDtos.MaintenanceRequest req, String actor) {
        Vehicle v = find(id);
        MaintenanceRecord m = MaintenanceRecord.builder()
                .vehicle(v)
                .serviceDate(req.getServiceDate() != null ? req.getServiceDate() : LocalDate.now())
                .description(req.getDescription())
                .cost(req.getCost())
                .build();
        maintenanceRecordRepository.save(m);
        v.setMaintenanceNotes(req.getDescription());
        auditService.log(actor, "CREATE", "Maintenance", String.valueOf(m.getId()), req.getDescription());
        return m;
    }

    public Vehicle find(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ApiException("Vehicle not found", HttpStatus.NOT_FOUND));
    }

    private Vehicle map(Vehicle v, VehicleDtos.Request req) {
        v.setRegistrationNumber(req.getRegistrationNumber());
        v.setMake(req.getMake());
        v.setModel(req.getModel());
        v.setCapacity(req.getCapacity() != null ? req.getCapacity() : 40);
        v.setStatus(req.getStatus() != null ? req.getStatus() : "ACTIVE");
        v.setFuelLevel(req.getFuelLevel() != null ? req.getFuelLevel() : 100.0);
        v.setGpsStatus(req.getGpsStatus() != null ? req.getGpsStatus() : "ONLINE");
        v.setLatitude(req.getLatitude());
        v.setLongitude(req.getLongitude());
        v.setMaintenanceDue(req.getMaintenanceDue());
        v.setMaintenanceNotes(req.getMaintenanceNotes());
        if (req.getAssignedRouteId() != null) {
            v.setAssignedRoute(routeRepository.findById(req.getAssignedRouteId())
                    .orElseThrow(() -> new ApiException("Route not found", HttpStatus.NOT_FOUND)));
        } else {
            v.setAssignedRoute(null);
        }
        if (req.getAssignedDriverId() != null) {
            v.setAssignedDriver(driverRepository.findById(req.getAssignedDriverId())
                    .orElseThrow(() -> new ApiException("Driver not found", HttpStatus.NOT_FOUND)));
        } else {
            v.setAssignedDriver(null);
        }
        return v;
    }
}
