package com.transitops.backend.dto;

import com.transitops.backend.entity.Vehicle;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

public final class VehicleDtos {
    private VehicleDtos() {}

    @Data
    public static class Request {
        @NotBlank
        private String registrationNumber;
        private String make;
        private String model;
        private Integer capacity;
        private String status;
        private Double fuelLevel;
        private String gpsStatus;
        private Double latitude;
        private Double longitude;
        private LocalDate maintenanceDue;
        private String maintenanceNotes;
        private Long assignedRouteId;
        private Long assignedDriverId;
    }

    @Data
    @Builder
    public static class Response {
        private Long id;
        private String registrationNumber;
        private String make;
        private String model;
        private Integer capacity;
        private String status;
        private Double fuelLevel;
        private String gpsStatus;
        private Double latitude;
        private Double longitude;
        private LocalDate maintenanceDue;
        private String maintenanceNotes;
        private Long assignedRouteId;
        private Long assignedDriverId;
        private Instant lastGpsAt;

        public static Response from(Vehicle v) {
            return Response.builder()
                    .id(v.getId())
                    .registrationNumber(v.getRegistrationNumber())
                    .make(v.getMake())
                    .model(v.getModel())
                    .capacity(v.getCapacity())
                    .status(v.getStatus())
                    .fuelLevel(v.getFuelLevel())
                    .gpsStatus(v.getGpsStatus())
                    .latitude(v.getLatitude())
                    .longitude(v.getLongitude())
                    .maintenanceDue(v.getMaintenanceDue())
                    .maintenanceNotes(v.getMaintenanceNotes())
                    .assignedRouteId(v.getAssignedRoute() != null ? v.getAssignedRoute().getId() : null)
                    .assignedDriverId(v.getAssignedDriver() != null ? v.getAssignedDriver().getId() : null)
                    .lastGpsAt(v.getLastGpsAt())
                    .build();
        }
    }

    @Data
    public static class GpsPing {
        private Double latitude;
        private Double longitude;
        private Double fuelLevel;
    }

    @Data
    public static class MaintenanceRequest {
        private LocalDate serviceDate;
        private String description;
        private Double cost;
    }
}
