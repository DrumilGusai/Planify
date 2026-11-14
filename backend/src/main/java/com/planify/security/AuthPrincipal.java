package com.planify.security;

import com.planify.entity.PlatformRole;

public record AuthPrincipal(
        Long userId,
        String email,
        PlatformRole role
) {}
