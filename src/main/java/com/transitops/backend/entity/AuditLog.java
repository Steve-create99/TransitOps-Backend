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
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String actorEmail;

    @Column(nullable = false)
    private String action;

    private String entityType;

    private String entityId;

    @Column(length = 2000)
    private String details;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
