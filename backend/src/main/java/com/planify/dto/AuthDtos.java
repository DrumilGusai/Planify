package com.planify.dto;

import com.planify.entity.PlatformRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AuthDtos {

    private AuthDtos() {}

    public record RegisterReq(
            @NotBlank(message = "Name is required")
            @Size(max = 100, message = "Name must be at most 100 characters")
            String name,

            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            @Size(max = 150, message = "Email must be at most 150 characters")
            String email,

            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
            String password
    ) {}

    public record LoginReq(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,

            @NotBlank(message = "Password is required")
            String password
    ) {}

    public record AccessRes(String accessToken) {}

    public record TokenPair(String accessToken, String refreshToken, Long userId) {}

    public record UserDto(
            Long id,
            String name,
            String email,
            PlatformRole platformRole,
            boolean active,
            Instant createdAt
    ) {}
}
