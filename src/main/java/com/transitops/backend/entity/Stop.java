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
@Table(name = "stops")
public class Stop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String zone;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Builder.Default
    private Integer averageRiders = 0;

    @Builder.Default
    private boolean wheelchairAccessible = true;

    private String amenities;

    private String status;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt;

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
