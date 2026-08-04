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
@Table(name = "drivers")
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String email;

    private String phone;

    private String photoUrl;

    @Column(unique = true)
    private String licenseNumber;

    private LocalDate licenseExpiry;

    @Builder.Default
    private String employmentStatus = "ACTIVE";

    private String availability;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_route_id")
    private RouteEntity assignedRoute;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt;

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
