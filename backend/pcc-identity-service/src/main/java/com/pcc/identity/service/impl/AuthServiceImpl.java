package com.pcc.identity.service.impl;

import com.pcc.identity.audit.AuditService;
import com.pcc.identity.config.SecurityProperties;
import com.pcc.identity.dto.request.ChangePasswordRequest;
import com.pcc.identity.dto.request.ForgotPasswordRequest;
import com.pcc.identity.dto.request.LoginRequest;
import com.pcc.identity.dto.request.ResetPasswordRequest;
import com.pcc.identity.dto.response.AuthResponse;
import com.pcc.identity.dto.response.LoginHistoryResponse;
import com.pcc.identity.dto.response.MessageResponse;
import com.pcc.identity.dto.response.PageResponse;
import com.pcc.identity.dto.response.UserResponse;
import com.pcc.identity.entity.PasswordResetToken;
import com.pcc.identity.entity.RefreshToken;
import com.pcc.identity.entity.User;
import com.pcc.identity.enums.AccountStatus;
import com.pcc.identity.enums.LoginEventType;
import com.pcc.identity.exception.InvalidTokenException;
import com.pcc.identity.exception.ResourceNotFoundException;
import com.pcc.identity.mapper.LoginHistoryMapper;
import com.pcc.identity.mapper.UserMapper;
import com.pcc.identity.repository.LoginHistoryRepository;
import com.pcc.identity.repository.PasswordResetTokenRepository;
import com.pcc.identity.repository.UserRepository;
import com.pcc.identity.security.JwtService;
import com.pcc.identity.service.AuthService;
import com.pcc.identity.service.IssuedRefreshToken;
import com.pcc.identity.service.LoginHistoryService;
import com.pcc.identity.service.RefreshTokenService;
import com.pcc.identity.util.TokenHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid username or password";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final LoginHistoryService loginHistoryService;
    private final AuditService auditService;
    private final SecurityProperties properties;

    /**
     * Deliberately NOT transactional: each bookkeeping write (failed-attempt
     * counter, lock timestamp, last login) must commit independently even when
     * the method exits via a thrown authentication exception. Entities are
     * loaded eagerly (roles + permissions entity graph) so no lazy access is
     * needed outside a transaction.
     */
    @Override
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        User user = userRepository.findByUsernameOrEmail(request.username().trim()).orElse(null);

        if (user == null) {
            record(null, request.username(), LoginEventType.LOGIN_FAILED, ipAddress, userAgent, "Unknown username or email");
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        String username = user.getUsername();

        if (user.getAccountStatus() == AccountStatus.INACTIVE) {
            record(user.getId(), username, LoginEventType.LOGIN_FAILED, ipAddress, userAgent, "Account inactive");
            throw new DisabledException("Account is deactivated. Contact an administrator.");
        }

        if (user.isLocked()) {
            record(user.getId(), username, LoginEventType.LOGIN_FAILED, ipAddress, userAgent, "Account locked");
            throw new LockedException("Account is temporarily locked until " + user.getAccountLockedUntil());
        }

        // An expired lock window clears automatically on next login attempt.
        if (user.getAccountLockedUntil() != null && !user.isLocked()) {
            user.setAccountLockedUntil(null);
            user.setFailedLoginAttempts(0);
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= properties.getLockout().getMaxFailedAttempts()) {
                user.setAccountLockedUntil(
                        Instant.now().plus(properties.getLockout().getLockDurationMinutes(), ChronoUnit.MINUTES));
                user.setFailedLoginAttempts(0);
                userRepository.save(user);
                record(user.getId(), username, LoginEventType.ACCOUNT_LOCKED, ipAddress, userAgent,
                        "Max failed attempts reached");
                throw new LockedException("Too many failed attempts. Account locked for "
                        + properties.getLockout().getLockDurationMinutes() + " minutes.");
            }
            userRepository.save(user);
            record(user.getId(), username, LoginEventType.LOGIN_FAILED, ipAddress, userAgent, "Invalid password");
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        record(user.getId(), username, LoginEventType.LOGIN_SUCCESS, ipAddress, userAgent, null);
        return buildAuthResponse(user, ipAddress, userAgent);
    }

    @Override
    @Transactional
    public AuthResponse refresh(String rawRefreshToken, String ipAddress, String userAgent) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidTokenException("Refresh token is required");
        }

        RefreshToken stored = refreshTokenService.requireActive(rawRefreshToken);
        User user = stored.getUser();

        if (user.getAccountStatus() != AccountStatus.ACTIVE || user.isLocked()) {
            throw new InvalidTokenException("Account is no longer active");
        }

        // Rotate: revoke the presented token, issue a fresh one.
        IssuedRefreshToken issued = refreshTokenService.issue(user, ipAddress, userAgent);
        stored.setRevoked(true);
        stored.setRevokedAt(Instant.now());
        stored.setReplacedById(issued.id());

        record(user.getId(), user.getUsername(), LoginEventType.TOKEN_REFRESH, ipAddress, userAgent, null);
        String accessToken = jwtService.generateAccessToken(user);
        return new AuthResponse(accessToken, "Bearer", jwtService.getAccessTokenTtlSeconds(),
                issued.rawValue(), UserMapper.toResponse(user));
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revoke(rawRefreshToken);
        }
        com.pcc.identity.util.SecurityUtils.currentUser().ifPresent(p ->
                record(p.getId(), p.getUsername(), LoginEventType.LOGOUT, null, null, null));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse currentUser(Long userId) {
        User user = userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserMapper.toResponse(user);
    }

    @Override
    @Transactional
    public MessageResponse changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);

        // Invalidate all sessions so other devices must re-authenticate.
        refreshTokenService.revokeAllForUser(userId);
        record(user.getId(), user.getUsername(), LoginEventType.PASSWORD_CHANGED, null, null, null);
        auditService.record("PASSWORD_CHANGE", "USER", String.valueOf(userId), "User changed own password");
        return new MessageResponse("Password changed successfully");
    }

    @Override
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        // Always return the same response to avoid account enumeration.
        userRepository.findByUsernameOrEmail(request.identifier().trim()).ifPresent(user -> {
            String raw = generateSecureToken();
            PasswordResetToken token = new PasswordResetToken();
            token.setUser(user);
            token.setTokenHash(TokenHashUtil.sha256(raw));
            token.setExpiresAt(Instant.now().plus(properties.getPasswordResetTokenTtlMinutes(), ChronoUnit.MINUTES));
            passwordResetTokenRepository.save(token);
            // Never expose the bearer token through logs. Until mail delivery exists,
            // recovery must use the authenticated administrator reset flow.
            log.warn("Self-service password reset requested but delivery is not configured.");
        });
        return new MessageResponse("If the account exists, password reset instructions have been sent.");
    }

    @Override
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository
                .findByTokenHash(TokenHashUtil.sha256(request.token()))
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired reset token"));

        if (!token.isUsable()) {
            throw new InvalidTokenException("Invalid or expired reset token");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);

        token.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(token);
        refreshTokenService.revokeAllForUser(user.getId());
        record(user.getId(), user.getUsername(), LoginEventType.PASSWORD_RESET, null, null, null);
        auditService.record("PASSWORD_RESET", "USER", String.valueOf(user.getId()), "Password reset via token");
        return new MessageResponse("Password has been reset successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LoginHistoryResponse> loginHistory(Long userId, Pageable pageable) {
        return PageResponse.from(
                loginHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable),
                LoginHistoryMapper::toResponse);
    }

    private AuthResponse buildAuthResponse(User user, String ipAddress, String userAgent) {
        String accessToken = jwtService.generateAccessToken(user);
        IssuedRefreshToken refreshToken = refreshTokenService.issue(user, ipAddress, userAgent);
        return new AuthResponse(accessToken, "Bearer", jwtService.getAccessTokenTtlSeconds(),
                refreshToken.rawValue(), UserMapper.toResponse(user));
    }

    private void record(Long userId, String username, LoginEventType type,
                        String ip, String userAgent, String reason) {
        try {
            loginHistoryService.record(userId, username, type, ip, userAgent, reason);
        } catch (Exception e) {
            log.warn("Failed to write login history for {}: {}", username, e.getMessage());
        }
    }

    private static String generateSecureToken() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
