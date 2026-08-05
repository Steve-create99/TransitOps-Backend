package com.transitops.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

public final class InviteDtos {
    private InviteDtos() {}

    @Data
    public static class DriverInviteRequest {
        @NotBlank
        @Email
        private String email;
        @NotBlank
        private String firstName;
        @NotBlank
        private String lastName;
        private String phone;
        private String licenseNumber;
        private Long assignedRouteId;
    }

    @Data
    public static class AcceptRequest {
        @NotBlank
        private String token;
        @NotBlank
        @Size(min = 8, max = 128)
        private String password;
    }

    @Data
    @Builder
    public static class InviteInfoResponse {
        private String email;
        private String firstName;
        private String lastName;
        private String role;
        private Instant expiresAt;
        private boolean valid;
        private String message;
    }

    @Data
    @Builder
    public static class InviteCreatedResponse {
        private Long userId;
        private Long driverId;
        private String email;
        private String status;
        private Instant expiresAt;
        private boolean emailSent;
        /** Accept link for admin copy/share when email delivery fails. */
        private String acceptUrl;
        private String message;
    }
}
