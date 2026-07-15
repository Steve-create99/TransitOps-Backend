package com.transitops.service;

import com.transitops.dto.AuthResponse;
import com.transitops.dto.LoginRequest;
import com.transitops.dto.RefreshRequest;
import com.transitops.dto.RegisterRequest;
import com.transitops.entity.User;
import com.transitops.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    // ── Register ──────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        userRepository.save(user);

        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        user.setAccessToken(accessToken);
        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        return buildAuthResponse(user, accessToken, refreshToken, "Account created successfully");
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        user.setAccessToken(accessToken);
        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        return buildAuthResponse(user, accessToken, refreshToken, "Login successful");
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        final String incomingRefresh = request.getRefreshToken();
        final String email           = jwtService.extractEmail(incomingRefresh);

        User user = (User) userDetailsService.loadUserByUsername(email);

        // 1. Verify token is cryptographically valid and not expired
        if (!jwtService.isRefreshTokenValid(incomingRefresh, user)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        // 2. Verify incoming refresh token matches the stored token (has not been revoked/overwritten)
        if (user.getRefreshToken() == null || !user.getRefreshToken().equals(incomingRefresh)) {
            throw new RuntimeException("Refresh token has been revoked");
        }

        // Issue new access token
        String newAccessToken = jwtService.generateAccessToken(user);
        user.setAccessToken(newAccessToken);
        userRepository.save(user);

        return buildAuthResponse(user, newAccessToken, incomingRefresh, "Token refreshed successfully");
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Transactional
    public void logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Authorization header missing or invalid");
        }

        final String accessToken = authHeader.substring(7);
        final String email = jwtService.extractEmail(accessToken);

        User user = (User) userDetailsService.loadUserByUsername(email);

        // Revoke active sessions by clearing tokens
        user.setAccessToken(null);
        user.setRefreshToken(null);
        userRepository.save(user);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(User user, String accessToken,
                                           String refreshToken, String message) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getAccessExpirationMs())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .message(message)
                .build();
    }
}
