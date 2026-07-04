package com.transitops.repository;

import com.transitops.entity.Token;
import com.transitops.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {

    Optional<Token> findByToken(String token);

    /** All tokens for a user that are neither revoked nor expired */
    @Query("SELECT t FROM Token t WHERE t.user.id = :userId AND t.revoked = false AND t.expired = false")
    List<Token> findAllValidTokensByUserId(@Param("userId") Long userId);

    /** Bulk-revoke all active tokens for a user (called before issuing new ones) */
    @Modifying
    @Query("UPDATE Token t SET t.revoked = true WHERE t.user.id = :userId AND t.revoked = false")
    void revokeAllByUserId(@Param("userId") Long userId);

    /** Find all tokens belonging to a user (for cleanup / admin) */
    List<Token> findAllByUser(User user);
}
