package com.planify.service;

import com.planify.audit.AuditEvent;
import com.planify.audit.AuditWriter;
import com.planify.dto.AuthDtos.LoginReq;
import com.planify.dto.AuthDtos.RegisterReq;
import com.planify.dto.AuthDtos.TokenPair;
import com.planify.dto.AuthDtos.UserDto;
import com.planify.entity.PlatformRole;
import com.planify.entity.RefreshToken;
import com.planify.entity.User;
import com.planify.exception.ConflictException;
import com.planify.exception.NotFoundException;
import com.planify.exception.UnauthorizedException;
import com.planify.mapper.UserMapper;
import com.planify.repository.RefreshTokenRepository;
import com.planify.repository.UserRepository;
import com.planify.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final AuditWriter auditWriter;

    @Value("${app.jwt.refresh-ttl-days}")
    private long refreshTtlDays;

    @Transactional
    public UserDto register(RegisterReq req, String ipAddress) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ConflictException("Email already registered");
        }
        User user = new User();
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setPlatformRole(PlatformRole.USER);
        user.setActive(true);
        User saved = users.save(user);

        auditWriter.write(new AuditEvent(
                saved.getId(), null, null, "AUTH", saved.getId(),
                "CREATE", null, "{\"email\":\"" + saved.getEmail() + "\"}", ipAddress, Instant.now()
        ));
        return userMapper.toDto(saved);
    }

    @Transactional
    public TokenPair login(LoginReq req, String ipAddress) {
        String email = req.email().trim().toLowerCase();
        User user = users.findByEmail(email)
                .filter(User::isActive)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        TokenPair pair = issueTokenPair(user);
        auditWriter.write(new AuditEvent(
                user.getId(), null, null, "AUTH", user.getId(),
                "LOGIN", null, "{\"email\":\"" + user.getEmail() + "\"}", ipAddress, Instant.now()
        ));
        return pair;
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public TokenPair refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new UnauthorizedException("Missing refresh token");
        }
        RefreshToken token = refreshTokens.findByTokenHash(sha256(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (token.isRevoked()) {
            refreshTokens.revokeAllByUser(token.getUserId());
            throw new UnauthorizedException("Refresh token reuse detected");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token expired");
        }

        token.setRevoked(true);
        refreshTokens.save(token);

        User user = users.findById(token.getUserId())
                .filter(User::isActive)
                .orElseThrow(() -> new UnauthorizedException("User account is inactive"));

        return issueTokenPair(user);
    }

    @Transactional
    public void logout(String rawToken, Long actorId, String ipAddress) {
        if (rawToken != null && !rawToken.isBlank()) {
            refreshTokens.findByTokenHash(sha256(rawToken)).ifPresent(t -> {
                t.setRevoked(true);
                refreshTokens.save(t);
                Long uid = actorId != null ? actorId : t.getUserId();
                auditWriter.write(new AuditEvent(
                        uid, null, null, "AUTH", uid,
                        "LOGOUT", null, null, ipAddress, Instant.now()
                ));
            });
        }
    }

    @Transactional(readOnly = true)
    public UserDto me(Long userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return userMapper.toDto(user);
    }

    private TokenPair issueTokenPair(User user) {
        String raw = generateRandomToken();
        RefreshToken entity = new RefreshToken();
        entity.setUserId(user.getId());
        entity.setTokenHash(sha256(raw));
        entity.setExpiresAt(Instant.now().plus(refreshTtlDays, ChronoUnit.DAYS));
        entity.setRevoked(false);
        entity.setCreatedAt(Instant.now());
        refreshTokens.save(entity);

        String accessToken = jwtService.generate(user);
        return new TokenPair(accessToken, raw, user.getId());
    }

    private String generateRandomToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
