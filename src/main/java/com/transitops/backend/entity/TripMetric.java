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
@Table(name = "trip_metrics")
public class TripMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private RouteEntity route;

    @Column(nullable = false)
    private LocalDate metricDate;

    @Builder.Default
    private Integer passengers = 0;

    @Builder.Default
    private Integer completedTrips = 0;

    @Builder.Default
    private Integer delayedTrips = 0;

    @Builder.Default
    private Double averageSpeed = 0.0;

    @Builder.Default
    private Double onTimePercentage = 100.0;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
