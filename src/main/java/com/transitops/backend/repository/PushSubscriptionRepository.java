package com.transitops.backend.repository;

import com.transitops.backend.entity.PushSubscription;
import com.transitops.backend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {
    Optional<PushSubscription> findByEndpoint(String endpoint);

    List<PushSubscription> findByUserId(Long userId);

    @Query("select s from PushSubscription s join fetch s.user u where u.role = :role and u.enabled = true")
    List<PushSubscription> findByUserRole(Role role);

    void deleteByEndpoint(String endpoint);

    void deleteByUserId(Long userId);
}