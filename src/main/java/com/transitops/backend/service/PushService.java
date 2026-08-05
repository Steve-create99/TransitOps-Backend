package com.transitops.backend.service;

import com.transitops.backend.entity.PushSubscription;
import com.transitops.backend.entity.Role;
import com.transitops.backend.entity.User;
import com.transitops.backend.exception.ApiException;
import com.transitops.backend.repository.PushSubscriptionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushService {

    private final PushSubscriptionRepository subscriptionRepository;

    @Value("${transitops.push.vapid-public-key:}")
    private String vapidPublicKey;

    @Value("${transitops.push.vapid-private-key:}")
    private String vapidPrivateKey;

    @Value("${transitops.push.vapid-subject:mailto:admin@transitops.local}")
    private String vapidSubject;

    private nl.martijndwars.webpush.PushService pushClient;

    @PostConstruct
    void init() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        if (isConfigured()) {
            try {
                pushClient = new nl.martijndwars.webpush.PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
                log.info("Web Push VAPID configured");
            } catch (Exception ex) {
                log.error("Failed to initialize Web Push client: {}", ex.getMessage());
                pushClient = null;
            }
        } else {
            log.warn("VAPID keys not set — web push disabled");
        }
    }

    public boolean isConfigured() {
        return vapidPublicKey != null && !vapidPublicKey.isBlank()
                && vapidPrivateKey != null && !vapidPrivateKey.isBlank();
    }

    public String getPublicKey() {
        if (!isConfigured()) {
            throw new ApiException("Web push is not configured", HttpStatus.SERVICE_UNAVAILABLE);
        }
        return vapidPublicKey;
    }

    @Transactional
    public PushSubscription subscribe(User user, String endpoint, String p256dh, String auth, String userAgent) {
        if (endpoint == null || endpoint.isBlank() || p256dh == null || auth == null) {
            throw new ApiException("Invalid push subscription", HttpStatus.BAD_REQUEST);
        }
        PushSubscription sub = subscriptionRepository.findByEndpoint(endpoint).orElse(null);
        if (sub == null) {
            sub = PushSubscription.builder()
                    .user(user)
                    .endpoint(endpoint)
                    .p256dh(p256dh)
                    .auth(auth)
                    .userAgent(userAgent)
                    .build();
        } else {
            sub.setUser(user);
            sub.setP256dh(p256dh);
            sub.setAuth(auth);
            if (userAgent != null) sub.setUserAgent(userAgent);
        }
        return subscriptionRepository.save(sub);
    }

    @Transactional
    public void unsubscribe(User user, String endpoint) {
        subscriptionRepository.findByEndpoint(endpoint).ifPresent(sub -> {
            if (sub.getUser() != null && sub.getUser().getId().equals(user.getId())) {
                subscriptionRepository.delete(sub);
            }
        });
    }

    public void notifyUser(Long userId, String title, String body, String url) {
        if (pushClient == null || userId == null) return;
        List<PushSubscription> subs = subscriptionRepository.findByUserId(userId);
        for (PushSubscription sub : subs) {
            sendOne(sub, title, body, url);
        }
    }

    public void notifyRole(Role role, String title, String body, String url) {
        if (pushClient == null || role == null) return;
        List<PushSubscription> subs = subscriptionRepository.findByUserRole(role);
        for (PushSubscription sub : subs) {
            sendOne(sub, title, body, url);
        }
    }

    private void sendOne(PushSubscription sub, String title, String body, String url) {
        try {
            String payload = """
                    {"title":%s,"body":%s,"url":%s,"icon":"/favicon.svg"}
                    """.formatted(
                    jsonString(title != null ? title : "TransitOps"),
                    jsonString(body != null ? body : ""),
                    jsonString(url != null ? url : "/notifications")
            ).trim();

            Notification notification = new Notification(
                    sub.getEndpoint(),
                    sub.getP256dh(),
                    sub.getAuth(),
                    payload.getBytes(StandardCharsets.UTF_8)
            );
            var response = pushClient.send(notification);
            int status = response.getStatusLine().getStatusCode();
            if (status == 404 || status == 410) {
                subscriptionRepository.delete(sub);
                log.info("Removed expired push subscription {}", sub.getId());
            } else if (status >= 400) {
                log.warn("Push send failed HTTP {} for sub {}", status, sub.getId());
            }
        } catch (Exception ex) {
            log.warn("Push send error for sub {}: {}", sub.getId(), ex.getMessage());
        }
    }

    private static String jsonString(String value) {
        if (value == null) return "null";
        String escaped = value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
        return "\"" + escaped + "\"";
    }
}
