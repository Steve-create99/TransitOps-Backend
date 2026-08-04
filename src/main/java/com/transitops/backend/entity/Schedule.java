package com.transitops.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "schedules")
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "route_id")
    private RouteEntity route;

    private LocalDate serviceDate;

    private LocalTime departureTime;

    private LocalTime arrivalTime;

    @Builder.Default
    private boolean weekdays = true;

    @Builder.Default
    private boolean weekends = false;

    @Builder.Default
    private boolean holidays = false;

    @Builder.Default
    private String delayStatus = "ON_TIME";

    private Integer delayMinutes;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Builder.Default
    private String status = "SCHEDULED";

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt;

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
