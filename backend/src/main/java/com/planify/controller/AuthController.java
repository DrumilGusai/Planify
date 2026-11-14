package com.planify.controller;

import com.planify.dto.AuthDtos.AccessRes;
import com.planify.dto.AuthDtos.LoginReq;
import com.planify.dto.AuthDtos.RegisterReq;
import com.planify.dto.AuthDtos.TokenPair;
import com.planify.dto.AuthDtos.UserDto;
import com.planify.security.AuthPrincipal;
import com.planify.security.LoginRateLimiter;
import com.planify.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "rt";

    private final AuthService authService;
    private final LoginRateLimiter rateLimiter;

    @Value("${app.cookie-secure:false}")
    private boolean cookieSecure;

    @Value("${app.jwt.refresh-ttl-days:7}")
    private long refreshTtlDays;

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(
            @Valid @RequestBody RegisterReq req,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(req, request.getRemoteAddr()));
    }

    @PostMapping("/login")
    public ResponseEntity<AccessRes> login(
            @Valid @RequestBody LoginReq req,
            HttpServletRequest request,
            HttpServletResponse response) {
        String key = request.getRemoteAddr() + "|" + req.email().trim().toLowerCase();
        rateLimiter.check(key);

        TokenPair pair = authService.login(req, request.getRemoteAddr());
        setRefreshCookie(response, pair.refreshToken(), (int) (refreshTtlDays * 24 * 3600));
        return ResponseEntity.ok(new AccessRes(pair.accessToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AccessRes> refresh(
            @CookieValue(REFRESH_COOKIE_NAME) String refreshToken,
            HttpServletResponse response) {
        TokenPair pair = authService.refresh(refreshToken);
        setRefreshCookie(response, pair.refreshToken(), (int) (refreshTtlDays * 24 * 3600));
        return ResponseEntity.ok(new AccessRes(pair.accessToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest request,
            HttpServletResponse response) {
        authService.logout(refreshToken, principal != null ? principal.userId() : null, request.getRemoteAddr());
        setRefreshCookie(response, "", 0);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(authService.me(principal.userId()));
    }

    private void setRefreshCookie(HttpServletResponse response, String value, int maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(maxAgeSeconds)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
