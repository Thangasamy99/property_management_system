package com.pcc.identity.controller;

import com.pcc.identity.config.SecurityProperties;
import com.pcc.identity.dto.request.ChangePasswordRequest;
import com.pcc.identity.dto.request.ForgotPasswordRequest;
import com.pcc.identity.dto.request.LoginRequest;
import com.pcc.identity.dto.request.LogoutRequest;
import com.pcc.identity.dto.request.RefreshTokenRequest;
import com.pcc.identity.dto.request.ResetPasswordRequest;
import com.pcc.identity.dto.response.AuthResponse;
import com.pcc.identity.dto.response.LoginHistoryResponse;
import com.pcc.identity.dto.response.MessageResponse;
import com.pcc.identity.dto.response.PageResponse;
import com.pcc.identity.dto.response.UserResponse;
import com.pcc.identity.security.UserPrincipal;
import com.pcc.identity.service.AuthService;
import com.pcc.identity.util.HttpRequestUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@Tag(name = "Authentication", description = "Login, tokens, password and session endpoints")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SecurityProperties properties;

    @Operation(summary = "Employee login (username or email + password)",
            description = "Returns a Bearer access token and sets the refresh token as an HttpOnly cookie (also returned in the body for non-browser clients).")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest http,
                                              HttpServletResponse response) {
        AuthResponse auth = authService.login(request,
                HttpRequestUtil.clientIp(http), HttpRequestUtil.userAgent(http));
        setRefreshCookie(response, auth.refreshToken());
        return ResponseEntity.ok(auth);
    }

    @Operation(summary = "Rotate the refresh token and obtain a new access token")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody(required = false) RefreshTokenRequest body,
                                                HttpServletRequest http,
                                                HttpServletResponse response) {
        String raw = resolveRefreshToken(http, body != null ? body.refreshToken() : null);
        AuthResponse auth = authService.refresh(raw,
                HttpRequestUtil.clientIp(http), HttpRequestUtil.userAgent(http));
        setRefreshCookie(response, auth.refreshToken());
        return ResponseEntity.ok(auth);
    }

    @Operation(summary = "Logout - revokes the refresh token and clears the cookie")
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@RequestBody(required = false) LogoutRequest body,
                                                  HttpServletRequest http,
                                                  HttpServletResponse response) {
        String raw = resolveRefreshToken(http, body != null ? body.refreshToken() : null);
        authService.logout(raw);
        clearRefreshCookie(response);
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }

    @Operation(summary = "Current logged-in user profile, roles and permissions")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.currentUser(principal.getId()));
    }

    @Operation(summary = "Change own password (revokes all sessions)")
    @PostMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                                          @Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(authService.changePassword(principal.getId(), request));
    }

    @Operation(summary = "Request a password reset token (always returns the same response)")
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @Operation(summary = "Reset password using a reset token")
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @Operation(summary = "Own login history (paginated)")
    @GetMapping("/login-history")
    public ResponseEntity<PageResponse<LoginHistoryResponse>> loginHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(authService.loginHistory(principal.getId(), pageable));
    }

    // ---- refresh cookie helpers ----

    private String resolveRefreshToken(HttpServletRequest request, String bodyToken) {
        if (bodyToken != null && !bodyToken.isBlank()) {
            return bodyToken;
        }
        return HttpRequestUtil.cookieValue(request, properties.getCookie().getRefreshName());
    }

    private void setRefreshCookie(HttpServletResponse response, String rawToken) {
        ResponseCookie cookie = ResponseCookie
                .from(properties.getCookie().getRefreshName(), rawToken)
                .httpOnly(true)
                .secure(properties.getCookie().isSecure())
                .sameSite(properties.getCookie().getSameSite())
                .path("/api/v1/auth")
                .maxAge(Duration.ofDays(properties.getJwt().getRefreshTokenTtlDays()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie
                .from(properties.getCookie().getRefreshName(), "")
                .httpOnly(true)
                .secure(properties.getCookie().isSecure())
                .sameSite(properties.getCookie().getSameSite())
                .path("/api/v1/auth")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
