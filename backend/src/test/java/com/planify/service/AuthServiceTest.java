package com.planify.service;

import com.planify.audit.AuditWriter;
import com.planify.dto.AuthDtos.LoginReq;
import com.planify.dto.AuthDtos.RegisterReq;
import com.planify.dto.AuthDtos.TokenPair;
import com.planify.dto.AuthDtos.UserDto;
import com.planify.entity.PlatformRole;
import com.planify.entity.RefreshToken;
import com.planify.entity.User;
import com.planify.exception.ConflictException;
import com.planify.exception.UnauthorizedException;
import com.planify.mapper.UserMapper;
import com.planify.repository.RefreshTokenRepository;
import com.planify.repository.UserRepository;
import com.planify.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository users;
    @Mock private RefreshTokenRepository refreshTokens;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private UserMapper userMapper;
    @Mock private AuditWriter auditWriter;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTtlDays", 7L);
    }

    @Test
    void register_throwsConflictWhenEmailExists() {
        when(users.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterReq("Alice", "Alice@Example.com", "Secret123!"), "127.0.0.1"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void register_encodesPasswordAndSavesUser() {
        when(users.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123!")).thenReturn("hashed");
        User saved = new User();
        saved.setId(1L);
        saved.setName("Alice");
        saved.setEmail("alice@example.com");
        saved.setPlatformRole(PlatformRole.USER);
        when(users.save(any(User.class))).thenReturn(saved);
        when(userMapper.toDto(saved)).thenReturn(new UserDto(1L, "Alice", "alice@example.com", PlatformRole.USER, true, Instant.now()));

        UserDto dto = authService.register(new RegisterReq("Alice", "Alice@Example.com", "Secret123!"), "127.0.0.1");

        assertThat(dto.email()).isEqualTo("alice@example.com");
        verify(auditWriter).write(any());
    }

    @Test
    void refresh_rotatesTokenAndRevokesOld() {
        RefreshToken existing = new RefreshToken();
        existing.setId(10L);
        existing.setUserId(1L);
        existing.setRevoked(false);
        existing.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));

        User user = new User();
        user.setId(1L);
        user.setEmail("alice@example.com");
        user.setActive(true);
        user.setPlatformRole(PlatformRole.USER);

        when(refreshTokens.findByTokenHash(any())).thenReturn(Optional.of(existing));
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(jwtService.generate(user)).thenReturn("new-jwt");

        TokenPair pair = authService.refresh("raw-token");

        assertThat(existing.isRevoked()).isTrue();
        assertThat(pair.accessToken()).isEqualTo("new-jwt");
        assertThat(pair.refreshToken()).isNotBlank();
        verify(refreshTokens).save(existing);
    }

    @Test
    void refresh_revokesAllUserTokensOnReuseDetection() {
        RefreshToken reused = new RefreshToken();
        reused.setId(10L);
        reused.setUserId(42L);
        reused.setRevoked(true);
        reused.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));

        when(refreshTokens.findByTokenHash(any())).thenReturn(Optional.of(reused));

        assertThatThrownBy(() -> authService.refresh("reused-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("reuse");

        verify(refreshTokens).revokeAllByUser(42L);
    }

    @Test
    void login_issuesTokenPairAndAudits() {
        User user = new User();
        user.setId(5L);
        user.setEmail("bob@example.com");
        user.setPasswordHash("bcrypt");
        user.setActive(true);
        user.setPlatformRole(PlatformRole.USER);

        when(users.findByEmail("bob@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Secret123!", "bcrypt")).thenReturn(true);
        when(jwtService.generate(user)).thenReturn("jwt-token");

        TokenPair pair = authService.login(new LoginReq("bob@example.com", "Secret123!"), "127.0.0.1");

        assertThat(pair.accessToken()).isEqualTo("jwt-token");
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokens).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(5L);
        verify(auditWriter).write(any());
    }
}
