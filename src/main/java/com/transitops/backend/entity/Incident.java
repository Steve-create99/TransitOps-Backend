package com.transitops.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "user", "assignedRoute", "assignedVehicle"})
    private Driver driver;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    private String category;

    private Double latitude;

    private Double longitude;

    @Builder.Default
    private String severity = "LOW";

    @Builder.Default
    private String status = "OPEN";

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
