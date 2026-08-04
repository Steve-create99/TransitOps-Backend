package com.transitops.backend.repository;

import com.transitops.backend.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserIdAndArchivedFalse(Long userId, Pageable pageable);
    Page<Notification> findByUserIdAndArchivedFalseAndCategoryIgnoreCase(Long userId, String category, Pageable pageable);
    long countByUserIdAndReadFlagFalseAndArchivedFalse(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.readFlag = true WHERE n.user.id = :userId AND n.readFlag = false")
    int markAllRead(@Param("userId") Long userId);
}
