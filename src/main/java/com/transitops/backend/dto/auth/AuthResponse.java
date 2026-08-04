package com.transitops.backend.dto.auth;

import com.transitops.backend.entity.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String token;
    private String refreshToken;
    private Long expiresIn;
    private UserDto user;
    private String email;
    private String firstName;
    private String lastName;
    private Role role;
    private String message;

    @Data
    @Builder
    public static class UserDto {
        private Long id;
        private String email;
        private String firstName;
        private String lastName;
        private Role role;
    }
}
