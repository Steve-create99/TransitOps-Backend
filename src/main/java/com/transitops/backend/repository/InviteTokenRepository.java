package com.transitops.backend.repository;

import com.transitops.backend.entity.InviteToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InviteTokenRepository extends JpaRepository<InviteToken, Long> {
    Optional<InviteToken> findByToken(String token);
    Optional<InviteToken> findFirstByEmailIgnoreCaseAndUsedAtIsNullOrderByCreatedAtDesc(String email);
}
