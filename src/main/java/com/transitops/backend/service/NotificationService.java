package com.transitops.backend.service;

import com.transitops.backend.dto.common.PageResponse;
import com.transitops.backend.entity.Notification;
import com.transitops.backend.entity.User;
import com.transitops.backend.exception.ApiException;
import com.transitops.backend.repository.NotificationRepository;
import com.transitops.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PageResponse<Notification> list(User user, String category, String search, Pageable pageable) {
        Page<Notification> page = (category != null && !category.isBlank())
                ? notificationRepository.findByUserIdAndArchivedFalseAndCategoryIgnoreCase(user.getId(), category, pageable)
                : notificationRepository.findByUserIdAndArchivedFalse(user.getId(), pageable);
        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            var filtered = page.getContent().stream()
                    .filter(n -> n.getTitle().toLowerCase().contains(q) || n.getMessage().toLowerCase().contains(q))
                    .toList();
            return PageResponse.<Notification>builder()
                    .content(filtered)
                    .page(page.getNumber())
                    .size(page.getSize())
                    .totalElements(filtered.size())
                    .totalPages(1)
                    .last(true)
                    .build();
        }
        return PageResponse.from(page);
    }

    public long unreadCount(User user) {
        return notificationRepository.countByUserIdAndReadFlagFalseAndArchivedFalse(user.getId());
    }

    @Transactional
    public Notification markRead(Long id, User user) {
        Notification n = findOwned(id, user);
        n.setReadFlag(true);
        return n;
    }

    @Transactional
    public Map<String, Object> markAllRead(User user) {
        int updated = notificationRepository.markAllRead(user.getId());
        return Map.of("updated", updated);
    }

    @Transactional
    public Notification archive(Long id, User user) {
        Notification n = findOwned(id, user);
        n.setArchived(true);
        return n;
    }

    @Transactional
    public void delete(Long id, User user) {
        Notification n = findOwned(id, user);
        notificationRepository.delete(n);
    }

    @Transactional
    public Notification create(String title, String message, String category, String priority, Long userId) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        Notification n = Notification.builder()
                .title(title)
                .message(message)
                .category(category != null ? category : "GENERAL")
                .priority(priority != null ? priority : "MEDIUM")
                .user(target)
                .build();
        return notificationRepository.save(n);
    }

    private Notification findOwned(Long id, User user) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new ApiException("Notification not found", HttpStatus.NOT_FOUND));
        if (n.getUser() == null || !n.getUser().getId().equals(user.getId())) {
            throw new ApiException("Notification not found", HttpStatus.NOT_FOUND);
        }
        return n;
    }
}
