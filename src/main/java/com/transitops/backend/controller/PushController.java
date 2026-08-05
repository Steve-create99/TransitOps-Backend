package com.transitops.backend.controller;

import com.transitops.backend.entity.PushSubscription;
import com.transitops.backend.entity.User;
import com.transitops.backend.service.PushService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushController {

    private final PushService pushService;

    @GetMapping("/vapid-public-key")
    public Map<String, Object> vapidPublicKey() {
        return Map.of(
                "publicKey", pushService.getPublicKey(),
                "configured", pushService.isConfigured()
        );
    }

    @PostMapping("/subscribe")
    public Map<String, Object> subscribe(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "User-Agent", required = false) String userAgent
    ) {
        String endpoint = stringVal(body.get("endpoint"));
        Object keysObj = body.get("keys");
        String p256dh = null;
        String auth = null;
        if (keysObj instanceof Map<?, ?> keys) {
            p256dh = stringVal(keys.get("p256dh"));
            auth = stringVal(keys.get("auth"));
        }
        if (p256dh == null) p256dh = stringVal(body.get("p256dh"));
        if (auth == null) auth = stringVal(body.get("auth"));

        PushSubscription sub = pushService.subscribe(user, endpoint, p256dh, auth, userAgent);
        return Map.of("id", sub.getId(), "endpoint", sub.getEndpoint(), "ok", true);
    }

    @DeleteMapping("/subscribe")
    public ResponseEntity<Void> unsubscribe(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body
    ) {
        pushService.unsubscribe(user, body.get("endpoint"));
        return ResponseEntity.noContent().build();
    }

    private static String stringVal(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
