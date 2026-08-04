package com.transitops.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class DriverMeDtos {
    private DriverMeDtos() {}

    @Data
    @Builder
    public static class ProfileResponse {
        private Long id;
        private Long userId;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String photoUrl;
        private String licenseNumber;
        private LocalDate licenseExpiry;
        private String employmentStatus;
        private String availability;
        private String employeeId;
        private Long assignedRouteId;
        private String assignedRouteCode;
        private String assignedRouteName;
        private Long assignedVehicleId;
        private String assignedVehicle;
        private String assignedDepot;
    }

    @Data
    public static class ProfileUpdateRequest {
        private String phone;
        private String photoUrl;
    }

    @Data
    @Builder
    public static class ShiftResponse {
        private String shiftId;
        private String shiftName;
        private String startTime;
        private String endTime;
        private String busNumber;
        private String routeNumber;
        private String routeName;
        private String status;
        private Long routeId;
        private Long vehicleId;
        private List<String> stops;
    }

    @Data
    @Builder
    public static class AttendanceResponse {
        private Long id;
        private LocalDate date;
        private String checkInTime;
        private String checkOutTime;
        private Double shiftDurationHours;
        private String status;
        private Instant checkInAt;
        private Instant checkOutAt;
    }

    @Data
    public static class LocationRequest {
        private Double latitude;
        private Double longitude;
        private Double speed;
        private Double heading;
        private Double accuracy;
        private Long timestamp;
    }

    @Data
    public static class IncidentCreateRequest {
        private String title;
        private String category;
        private String description;
        private String severity;
        private Double latitude;
        private Double longitude;
    }

    @Data
    @Builder
    public static class TripResponse {
        private String id;
        private String routeId;
        private String routeName;
        private String routeNumber;
        private String busNumber;
        private String status;
        private int completedStopsCount;
        private int totalStopsCount;
        private String nextStopName;
        private long elapsedSeconds;
        private List<String> stops;
    }
}
