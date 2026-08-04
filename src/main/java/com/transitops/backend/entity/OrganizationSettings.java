package com.transitops.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "organization_settings")
public class OrganizationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    private String organizationName = "TransitOps";

    private String brandingPrimaryColor;

    private String brandingLogoUrl;

    private String contactEmail;

    private String timezone;

    @Builder.Default
    private boolean emailNotifications = true;

    @Builder.Default
    private boolean pushNotifications = true;

    @Builder.Default
    private int minPasswordLength = 8;

    @Builder.Default
    private boolean requireSpecialChar = true;

    private String apiBaseUrl;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt;

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
