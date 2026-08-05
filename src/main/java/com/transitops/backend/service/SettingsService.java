package com.transitops.backend.service;

import com.transitops.backend.dto.InviteDtos;
import com.transitops.backend.entity.OrganizationSettings;
import com.transitops.backend.entity.Role;
import com.transitops.backend.entity.User;
import com.transitops.backend.exception.ApiException;
import com.transitops.backend.repository.OrganizationSettingsRepository;
import com.transitops.backend.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class SettingsService {

    private final OrganizationSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final InviteService inviteService;

    public SettingsService(
            OrganizationSettingsRepository settingsRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService,
            @Lazy InviteService inviteService
    ) {
        this.settingsRepository = settingsRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.inviteService = inviteService;
    }

    public OrganizationSettings get() {
        return settingsRepository.findAll().stream().findFirst()
                .orElseGet(() -> settingsRepository.save(OrganizationSettings.builder().build()));
    }

    @Transactional
    public OrganizationSettings update(OrganizationSettings incoming, String actor) {
        OrganizationSettings s = get();
        if (incoming.getOrganizationName() != null) s.setOrganizationName(incoming.getOrganizationName());
        if (incoming.getBrandingPrimaryColor() != null) s.setBrandingPrimaryColor(incoming.getBrandingPrimaryColor());
        if (incoming.getBrandingLogoUrl() != null) s.setBrandingLogoUrl(incoming.getBrandingLogoUrl());
        if (incoming.getContactEmail() != null) s.setContactEmail(incoming.getContactEmail());
        if (incoming.getTimezone() != null) s.setTimezone(incoming.getTimezone());
        s.setEmailNotifications(incoming.isEmailNotifications());
        s.setPushNotifications(incoming.isPushNotifications());
        if (incoming.getMinPasswordLength() > 0) s.setMinPasswordLength(incoming.getMinPasswordLength());
        s.setRequireSpecialChar(incoming.isRequireSpecialChar());
        if (incoming.getApiBaseUrl() != null) s.setApiBaseUrl(incoming.getApiBaseUrl());
        auditService.log(actor, "UPDATE", "Settings", String.valueOf(s.getId()), "Organization settings updated");
        return s;
    }

    public Page<User> users(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * Invite a staff user. DRIVER role uses the email invite + accept flow.
     * Other roles get a pending disabled account (password set by admin or temporary).
     */
    @Transactional
    public Object inviteUser(Map<String, String> body, String actor) {
        Role role = Role.valueOf(body.getOrDefault("role", "DISPATCHER").toUpperCase());
        if (role == Role.DRIVER) {
            InviteDtos.DriverInviteRequest req = new InviteDtos.DriverInviteRequest();
            req.setEmail(body.get("email"));
            req.setFirstName(body.getOrDefault("firstName", "Invited"));
            req.setLastName(body.getOrDefault("lastName", "Driver"));
            req.setPhone(body.get("phone"));
            req.setLicenseNumber(body.get("licenseNumber"));
            if (body.get("assignedRouteId") != null && !body.get("assignedRouteId").isBlank()) {
                req.setAssignedRouteId(Long.valueOf(body.get("assignedRouteId")));
            }
            return inviteService.inviteDriver(req, actor);
        }

        String email = body.get("email");
        if (email == null || userRepository.existsByEmail(email.toLowerCase().trim())) {
            throw new ApiException("Email invalid or already registered", HttpStatus.CONFLICT);
        }
        User user = User.builder()
                .firstName(body.getOrDefault("firstName", "Invited"))
                .lastName(body.getOrDefault("lastName", "User"))
                .email(email.toLowerCase().trim())
                .password(passwordEncoder.encode(body.getOrDefault("password", "ChangeMe@123")))
                .role(role)
                .enabled(true)
                .build();
        userRepository.save(user);
        auditService.log(actor, "INVITE", "User", String.valueOf(user.getId()), role.name());
        return user;
    }

    @Transactional
    public User updateUser(Long id, Map<String, Object> body, String actor) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        if (body.containsKey("role")) user.setRole(Role.valueOf(String.valueOf(body.get("role"))));
        if (body.containsKey("enabled")) user.setEnabled(Boolean.parseBoolean(String.valueOf(body.get("enabled"))));
        if (body.containsKey("firstName")) user.setFirstName(String.valueOf(body.get("firstName")));
        if (body.containsKey("lastName")) user.setLastName(String.valueOf(body.get("lastName")));
        auditService.log(actor, "UPDATE", "User", String.valueOf(id), "User updated");
        return user;
    }
}
