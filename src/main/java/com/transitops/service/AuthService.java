package com.transitops.service;

import com.transitops.dto.AuthResponse;
import com.transitops.dto.LoginRequest;
import com.transitops.dto.RefreshRequest;
import com.transitops.dto.RegisterRequest;
import com.transitops.entity.Token;
import com.transitops.entity.TokenType;
import com.transitops.entity.User;
import com.transitops.repository.TokenRepository;
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
    private final TokenRepository tokenRepository;
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

        saveUserTokens(user, accessToken, refreshToken);

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

        // Revoke all existing tokens before issuing fresh ones (single active session)
        tokenRepository.revokeAllByUserId(user.getId());

        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        saveUserTokens(user, accessToken, refreshToken);

        return buildAuthResponse(user, accessToken, refreshToken, "Login successful");
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        final String incomingRefresh = request.getRefreshToken();
        final String email           = jwtService.extractEmail(incomingRefresh);

        User user = (User) userDetailsService.loadUserByUsername(email);

        // Validate JWT signature + type + not expired
        if (!jwtService.isRefreshTokenValid(incomingRefresh, user)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        // Validate token exists in DB and is not revoked
        Token storedRefresh = tokenRepository.findByToken(incomingRefresh)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        if (storedRefresh.isRevoked() || storedRefresh.isExpired()) {
            throw new RuntimeException("Refresh token has been revoked");
        }

        // Revoke the old access token for this user (leave refresh intact)
        tokenRepository.findAllValidTokensByUserId(user.getId())
                .stream()
                .filter(t -> t.getTokenType() == TokenType.ACCESS)
                .forEach(t -> t.setRevoked(true));

        // Issue a new access token and persist it
        String newAccessToken = jwtService.generateAccessToken(user);
        tokenRepository.save(Token.builder()
                .token(newAccessToken)
                .tokenType(TokenType.ACCESS)
                .user(user)
                .build());

        return buildAuthResponse(user, newAccessToken, incomingRefresh, "Token refreshed successfully");
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Transactional
    public void logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Authorization header missing or invalid");
        }

        final String accessToken = authHeader.substring(7);

        // Revoke the access token
        Token storedToken = tokenRepository.findByToken(accessToken)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        storedToken.setRevoked(true);

        // Also revoke all refresh tokens for this user for a full session logout
        final String email = jwtService.extractEmail(accessToken);
        User user = (User) userDetailsService.loadUserByUsername(email);
        tokenRepository.revokeAllByUserId(user.getId());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void saveUserTokens(User user, String accessToken, String refreshToken) {
        tokenRepository.save(Token.builder()
                .token(accessToken)
                .tokenType(TokenType.ACCESS)
                .user(user)
                .build());

        tokenRepository.save(Token.builder()
                .token(refreshToken)
                .tokenType(TokenType.REFRESH)
                .user(user)
                .build());
    }

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
