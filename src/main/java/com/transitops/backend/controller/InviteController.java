package com.transitops.backend.controller;

import com.transitops.backend.dto.InviteDtos;
import com.transitops.backend.dto.auth.AuthResponse;
import com.transitops.backend.service.InviteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invites")
@RequiredArgsConstructor
public class InviteController {

    private final InviteService inviteService;

    @GetMapping("/{token}")
    public InviteDtos.InviteInfoResponse peek(@PathVariable String token) {
        return inviteService.peek(token);
    }

    @PostMapping("/accept")
    public AuthResponse accept(@Valid @RequestBody InviteDtos.AcceptRequest request) {
        return inviteService.accept(request);
    }
}
