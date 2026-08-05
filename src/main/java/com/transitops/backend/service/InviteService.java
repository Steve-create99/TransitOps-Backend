package com.transitops.backend.service;

import com.transitops.backend.dto.InviteDtos;
import com.transitops.backend.dto.auth.AuthResponse;
import com.transitops.backend.entity.*;
import com.transitops.backend.exception.ApiException;
import com.transitops.backend.repository.DriverRepository;
import com.transitops.backend.repository.InviteTokenRepository;
import com.transitops.backend.repository.RouteRepository;
import com.transitops.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InviteService {

    private final InviteTokenRepository inviteTokenRepository;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final RouteRepository routeRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuthService authService;
    private final AuditService auditService;
    private final PushService pushService;
    private final NotificationService notificationService;

    @Value("${transitops.frontend-url:https://transitops-frontend.pages.dev}")
    private String frontendUrl;

    @Value("${transitops.invite.ttl-hours:72}")
    private long inviteTtlHours;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public InviteDtos.InviteCreatedResponse inviteDriver(InviteDtos.DriverInviteRequest req, String actorEmail) {
        String email = req.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            User existing = userRepository.findByEmail(email).orElseThrow();
            if (existing.isEnabled()) {
                throw new ApiException("A user with this email already exists", HttpStatus.CONFLICT);
            }
            // Re-invite: refresh token for pending account
            return reissueInvite(existing, req, actorEmail);
        }

        String tempPassword = randomSecret();
        User user = userRepository.save(User.builder()
                .firstName(req.getFirstName().trim())
                .lastName(req.getLastName().trim())
                .email(email)
                .password(passwordEncoder.encode(tempPassword))
                .role(Role.DRIVER)
                .enabled(false)
                .build());

        Driver driver = Driver.builder()
                .firstName(req.getFirstName().trim())
                .lastName(req.getLastName().trim())
                .email(email)
                .phone(req.getPhone())
                .licenseNumber(blankToNull(req.getLicenseNumber()))
                .employmentStatus("INVITED")
                .availability("OFF_DUTY")
                .user(user)
                .build();
        if (req.getAssignedRouteId() != null) {
            driver.setAssignedRoute(routeRepository.findById(req.getAssignedRouteId())
                    .orElseThrow(() -> new ApiException("Route not found", HttpStatus.NOT_FOUND)));
        }
        driverRepository.save(driver);

        InviteToken invite = createToken(user, driver, actorEmail);
        boolean emailSent = sendInviteEmail(invite);

        auditService.log(actorEmail, "INVITE", "Driver", String.valueOf(driver.getId()), email);
        pushService.notifyRole(Role.ADMIN, "Driver invite sent",
                req.getFirstName() + " " + req.getLastName() + " (" + email + ")",
                "/drivers");
        try {
            User admin = userRepository.findByEmail(actorEmail).orElse(null);
            if (admin != null) {
                notificationService.create(
                        "Driver invite sent",
                        "Invitation emailed to " + email,
                        "SYSTEM",
                        "LOW",
                        admin.getId()
                );
            }
        } catch (Exception ignored) {
            // non-fatal
        }

        return InviteDtos.InviteCreatedResponse.builder()
                .userId(user.getId())
                .driverId(driver.getId())
                .email(email)
                .status("INVITED")
                .expiresAt(invite.getExpiresAt())
                .emailSent(emailSent)
                .message(emailSent
                        ? "Invitation email sent"
                        : "Invite created but email was not sent (check RESEND_API_KEY)")
                .build();
    }

    private InviteDtos.InviteCreatedResponse reissueInvite(User user, InviteDtos.DriverInviteRequest req, String actorEmail) {
        if (user.getRole() != Role.DRIVER) {
            throw new ApiException("Email belongs to a non-driver account", HttpStatus.CONFLICT);
        }
        user.setFirstName(req.getFirstName().trim());
        user.setLastName(req.getLastName().trim());
        user.setEnabled(false);

        Driver driver = driverRepository.findByUserId(user.getId())
                .orElseGet(() -> driverRepository.save(Driver.builder()
                        .firstName(req.getFirstName().trim())
                        .lastName(req.getLastName().trim())
                        .email(user.getEmail())
                        .phone(req.getPhone())
                        .licenseNumber(blankToNull(req.getLicenseNumber()))
                        .employmentStatus("INVITED")
                        .availability("OFF_DUTY")
                        .user(user)
                        .build()));
        driver.setFirstName(req.getFirstName().trim());
        driver.setLastName(req.getLastName().trim());
        driver.setPhone(req.getPhone());
        if (req.getLicenseNumber() != null && !req.getLicenseNumber().isBlank()) {
            driver.setLicenseNumber(req.getLicenseNumber().trim());
        }
        driver.setEmploymentStatus("INVITED");

        InviteToken invite = createToken(user, driver, actorEmail);
        boolean emailSent = sendInviteEmail(invite);
        auditService.log(actorEmail, "REINVITE", "Driver", String.valueOf(driver.getId()), user.getEmail());

        return InviteDtos.InviteCreatedResponse.builder()
                .userId(user.getId())
                .driverId(driver.getId())
                .email(user.getEmail())
                .status("INVITED")
                .expiresAt(invite.getExpiresAt())
                .emailSent(emailSent)
                .message(emailSent ? "Invitation email resent" : "Invite refreshed but email was not sent")
                .build();
    }

    @Transactional(readOnly = true)
    public InviteDtos.InviteInfoResponse peek(String token) {
        InviteToken invite = inviteTokenRepository.findByToken(token)
                .orElse(null);
        if (invite == null) {
            return InviteDtos.InviteInfoResponse.builder()
                    .valid(false)
                    .message("Invite not found")
                    .build();
        }
        if (invite.isUsed()) {
            return InviteDtos.InviteInfoResponse.builder()
                    .valid(false)
                    .email(invite.getEmail())
                    .message("This invite has already been used")
                    .build();
        }
        if (invite.isExpired()) {
            return InviteDtos.InviteInfoResponse.builder()
                    .valid(false)
                    .email(invite.getEmail())
                    .expiresAt(invite.getExpiresAt())
                    .message("This invite has expired")
                    .build();
        }
        return InviteDtos.InviteInfoResponse.builder()
                .valid(true)
                .email(invite.getEmail())
                .firstName(invite.getFirstName())
                .lastName(invite.getLastName())
                .role(invite.getRole().name())
                .expiresAt(invite.getExpiresAt())
                .message("Invite is valid")
                .build();
    }

    @Transactional
    public AuthResponse accept(InviteDtos.AcceptRequest req) {
        InviteToken invite = inviteTokenRepository.findByToken(req.getToken())
                .orElseThrow(() -> new ApiException("Invalid invite token", HttpStatus.NOT_FOUND));
        if (invite.isUsed()) {
            throw new ApiException("This invite has already been used", HttpStatus.CONFLICT);
        }
        if (invite.isExpired()) {
            throw new ApiException("This invite has expired", HttpStatus.GONE);
        }
        if (req.getPassword() == null || req.getPassword().length() < 8) {
            throw new ApiException("Password must be at least 8 characters", HttpStatus.BAD_REQUEST);
        }

        User user = invite.getUser();
        if (user == null) {
            user = userRepository.findByEmail(invite.getEmail())
                    .orElseThrow(() -> new ApiException("Invited user missing", HttpStatus.NOT_FOUND));
        }
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setEnabled(true);
        user.setFirstName(invite.getFirstName());
        user.setLastName(invite.getLastName());
        user.setRole(Role.DRIVER);

        Driver driver = invite.getDriver();
        if (driver == null) {
            driver = driverRepository.findByUserId(user.getId()).orElse(null);
        }
        if (driver != null) {
            driver.setUser(user);
            driver.setEmploymentStatus("ACTIVE");
            driver.setAvailability("AVAILABLE");
            driver.setEmail(user.getEmail());
            driver.setFirstName(user.getFirstName());
            driver.setLastName(user.getLastName());
        }

        invite.setUsedAt(Instant.now());
        auditService.log(user.getEmail(), "ACCEPT_INVITE", "User", String.valueOf(user.getId()), "Driver activated");

        pushService.notifyRole(Role.ADMIN, "Driver activated",
                user.getFirstName() + " " + user.getLastName() + " accepted their invite",
                "/drivers");

        return authService.issueTokens(user, "Account activated. Welcome to TransitOps.");
    }

    private InviteToken createToken(User user, Driver driver, String actorEmail) {
        Instant expires = Instant.now().plus(Duration.ofHours(Math.max(1, inviteTtlHours)));
        InviteToken token = InviteToken.builder()
                .token(newToken())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(Role.DRIVER)
                .user(user)
                .driver(driver)
                .expiresAt(expires)
                .invitedBy(actorEmail)
                .build();
        return inviteTokenRepository.save(token);
    }

    private boolean sendInviteEmail(InviteToken invite) {
        String base = frontendUrl == null ? "https://transitops-frontend.pages.dev" : frontendUrl.replaceAll("/+$", "");
        String acceptUrl = base + "/invite/accept?token=" + invite.getToken();
        String expiresLabel = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm z")
                .withZone(ZoneId.of("Africa/Accra"))
                .format(invite.getExpiresAt());
        return emailService.sendDriverInvite(invite.getEmail(), invite.getFirstName(), acceptUrl, expiresLabel);
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String randomSecret() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(UUID.randomUUID().toString().getBytes());
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
