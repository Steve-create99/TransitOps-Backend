package com.transitops.backend.service;

import com.transitops.backend.dto.auth.*;
import com.transitops.backend.entity.RefreshToken;
import com.transitops.backend.entity.Role;
import com.transitops.backend.entity.User;
import com.transitops.backend.exception.ApiException;
import com.transitops.backend.repository.RefreshTokenRepository;
import com.transitops.backend.repository.UserRepository;
import com.transitops.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuditService auditService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.getRole() == Role.ADMIN) {
            throw new ApiException("Admin accounts cannot self-register. Contact an administrator.", HttpStatus.FORBIDDEN);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException("Email already registered", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .enabled(true)
                .build();
        userRepository.save(user);
        auditService.log(user.getEmail(), "REGISTER", "User", String.valueOf(user.getId()), "Self registration");
        return buildAuthResponse(user, "Account created successfully");
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase().trim(), request.getPassword())
        );
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        if (!user.isEnabled()) {
            throw new ApiException("Account is disabled", HttpStatus.FORBIDDEN);
        }
        auditService.log(user.getEmail(), "LOGIN", "User", String.valueOf(user.getId()), "Login success");
        return buildAuthResponse(user, "Login successful");
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new ApiException("Invalid refresh token", HttpStatus.UNAUTHORIZED));
        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException("Refresh token expired", HttpStatus.UNAUTHORIZED);
        }
        User user = stored.getUser();
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        return buildAuthResponse(user, "Token refreshed");
    }

    @Transactional
    public void logout(User user) {
        refreshTokenRepository.deleteByUser(user);
        auditService.log(user.getEmail(), "LOGOUT", "User", String.valueOf(user.getId()), "Logout");
    }

    public AuthResponse.UserDto me(User user) {
        return AuthResponse.UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .build();
    }

    private AuthResponse buildAuthResponse(User user, String message) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        refreshTokenRepository.save(RefreshToken.builder()
                .token(refreshToken)
                .user(user)
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshExpiration()))
                .revoked(false)
                .build());

        AuthResponse.UserDto userDto = me(user);
        return AuthResponse.builder()
                .accessToken(accessToken)
                .token(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getAccessExpiration())
                .user(userDto)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .message(message)
                .build();
    }
}
