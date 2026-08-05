package com.transitops.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JsonIgnore
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

    /** Factory that does not rely on Lombok {@code builder()} (safer under constrained CI heaps). */
    public static Driver invited(String firstName, String lastName, String email, String phone,
                                 String licenseNumber, User user) {
        Driver driver = new Driver();
        driver.setFirstName(firstName);
        driver.setLastName(lastName);
        driver.setEmail(email);
        driver.setPhone(phone);
        driver.setLicenseNumber(licenseNumber);
        driver.setEmploymentStatus("INVITED");
        driver.setAvailability("OFF_DUTY");
        driver.setUser(user);
        driver.setCreatedAt(Instant.now());
        return driver;
    }
}
