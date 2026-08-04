package com.transitops.backend.dto.auth;

import com.transitops.backend.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    @NotBlank
    @Email
    private String email;
    @NotNull
    private Role role;
    @NotBlank
    @Size(min = 8)
    private String password;
}
