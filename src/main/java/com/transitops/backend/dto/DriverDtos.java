package com.transitops.backend.dto;

import com.transitops.backend.entity.Driver;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

public final class DriverDtos {
    private DriverDtos() {}

    @Data
    public static class Request {
        @NotBlank
        private String firstName;
        @NotBlank
        private String lastName;
        private String email;
        private String phone;
        private String photoUrl;
        private String licenseNumber;
        private LocalDate licenseExpiry;
        private String employmentStatus;
        private String availability;
        private Long assignedRouteId;
        private Long userId;
    }

    @Data
    @Builder
    public static class Response {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String photoUrl;
        private String licenseNumber;
        private LocalDate licenseExpiry;
        private String employmentStatus;
        private String availability;
        private Long assignedRouteId;
        private String assignedRouteName;
        private Long userId;

        public static Response from(Driver d) {
            return Response.builder()
                    .id(d.getId())
                    .firstName(d.getFirstName())
                    .lastName(d.getLastName())
                    .email(d.getEmail())
                    .phone(d.getPhone())
                    .photoUrl(d.getPhotoUrl())
                    .licenseNumber(d.getLicenseNumber())
                    .licenseExpiry(d.getLicenseExpiry())
                    .employmentStatus(d.getEmploymentStatus())
                    .availability(d.getAvailability())
                    .assignedRouteId(d.getAssignedRoute() != null ? d.getAssignedRoute().getId() : null)
                    .assignedRouteName(d.getAssignedRoute() != null ? d.getAssignedRoute().getName() : null)
                    .userId(d.getUser() != null ? d.getUser().getId() : null)
                    .build();
        }
    }

    @Data
    public static class IncidentRequest {
        @NotBlank
        private String title;
        private String description;
        private String severity;
    }

    @Data
    public static class AttendanceRequest {
        private LocalDate date;
        private String status;
        private String notes;
    }
}
