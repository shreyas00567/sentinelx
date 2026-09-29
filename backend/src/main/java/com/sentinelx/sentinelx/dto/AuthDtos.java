package com.sentinelx.sentinelx.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AuthDtos {

    private AuthDtos() {}

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password) {}

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 64) String username,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @Size(max = 128) String fullName) {}

    public record UserDto(
            Long id,
            String username,
            String email,
            String fullName,
            String role,
            boolean enabled,
            Instant createdAt) {}

    public record AuthResponse(String token, UserDto user) {}
}
