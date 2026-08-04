package com.transitops.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String registrationNumber;

    private String make;

    private String model;

    @Builder.Default
    private Integer capacity = 40;

    @Builder.Default
    private String status = "ACTIVE";

    @Builder.Default
    private Double fuelLevel = 100.0;

    @Builder.Default
    private String gpsStatus = "ONLINE";

    private Double latitude;

    private Double longitude;

    private LocalDate maintenanceDue;

    private String maintenanceNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_route_id")
    private RouteEntity assignedRoute;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_driver_id")
    private Driver assignedDriver;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt;

    private Instant lastGpsAt;

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
