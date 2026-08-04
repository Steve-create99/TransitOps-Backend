package com.transitops.backend.service;

import com.transitops.backend.entity.OrganizationSettings;
import com.transitops.backend.entity.Role;
import com.transitops.backend.entity.User;
import com.transitops.backend.exception.ApiException;
import com.transitops.backend.repository.OrganizationSettingsRepository;
import com.transitops.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final OrganizationSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

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

    @Transactional
    public User inviteUser(Map<String, String> body, String actor) {
        String email = body.get("email");
        if (email == null || userRepository.existsByEmail(email)) {
            throw new ApiException("Email invalid or already registered", HttpStatus.CONFLICT);
        }
        Role role = Role.valueOf(body.getOrDefault("role", "DISPATCHER"));
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
