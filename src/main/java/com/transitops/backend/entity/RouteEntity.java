package com.transitops.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "routes")
public class RouteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String color;

    private String startStop;

    private String endStop;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "route_stops", joinColumns = @JoinColumn(name = "route_id"))
    @OrderColumn(name = "stop_order")
    @Column(name = "stop_name")
    @Builder.Default
    private List<String> intermediateStops = new ArrayList<>();

    @Builder.Default
    private String status = "Active";

    @Builder.Default
    private Integer frequencyMinutes = 15;

    @Builder.Default
    private Integer busCount = 0;

    private String type;

    private String direction;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt;

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
