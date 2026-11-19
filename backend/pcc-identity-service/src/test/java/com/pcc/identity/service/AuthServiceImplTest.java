package com.pcc.identity.service;

import com.pcc.identity.audit.AuditService;
import com.pcc.identity.config.SecurityProperties;
import com.pcc.identity.dto.request.ChangePasswordRequest;
import com.pcc.identity.dto.request.LoginRequest;
import com.pcc.identity.dto.request.ResetPasswordRequest;
import com.pcc.identity.dto.response.AuthResponse;
import com.pcc.identity.entity.PasswordResetToken;
import com.pcc.identity.entity.RefreshToken;
import com.pcc.identity.entity.User;
import com.pcc.identity.enums.AccountStatus;
import com.pcc.identity.exception.InvalidTokenException;
import com.pcc.identity.repository.LoginHistoryRepository;
import com.pcc.identity.repository.PasswordResetTokenRepository;
import com.pcc.identity.repository.UserRepository;
import com.pcc.identity.security.JwtService;
import com.pcc.identity.service.impl.AuthServiceImpl;
import com.pcc.identity.util.TokenHashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock LoginHistoryRepository loginHistoryRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock RefreshTokenService refreshTokenService;
    @Mock LoginHistoryService loginHistoryService;
    @Mock AuditService auditService;
    @Mock SecurityProperties properties;

    @InjectMocks
    AuthServiceImpl authService;

    private SecurityProperties.Lockout lockout;

    @BeforeEach
    void setUp() {
        lockout = new SecurityProperties.Lockout();
        lockout.setMaxFailedAttempts(5);
        lockout.setLockDurationMinutes(15);
        lenient().when(properties.getLockout()).thenReturn(lockout);
        lenient().when(properties.getPasswordResetTokenTtlMinutes()).thenReturn(30L);
    }

    private User activeUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("jdoe");
        user.setEmail("jdoe@pcc.cm");
        user.setPasswordHash("$2a$hash");
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setFailedLoginAttempts(0);
        return user;
    }

    @Test
    void loginSucceedsWithCorrectCredentials() {
        User user = activeUser();
        when(userRepository.findByUsernameOrEmail("jdoe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "$2a$hash")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access.jwt");
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(refreshTokenService.issue(eq(user), any(), any()))
                .thenReturn(new IssuedRefreshToken("refresh.raw", 10L));

        AuthResponse response = authService.login(new LoginRequest("jdoe", "secret"), "127.0.0.1", "JUnit");

        assertThat(response.accessToken()).isEqualTo("access.jwt");
        assertThat(response.refreshToken()).isEqualTo("refresh.raw");
        assertThat(response.user().username()).isEqualTo("jdoe");
        assertThat(user.getLastLoginAt()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void loginFailsForUnknownUsername() {
        when(userRepository.findByUsernameOrEmail("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "x"), "ip", "ua"))
                .isInstanceOf(BadCredentialsException.class);
        verify(loginHistoryService).record(eq(null), eq("ghost"), any(), any(), any(), anyString());
    }

    @Test
    void loginFailsForWrongPasswordAndCountsAttempts() {
        User user = activeUser();
        when(userRepository.findByUsernameOrEmail("jdoe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "$2a$hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("jdoe", "bad"), "ip", "ua"))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        verify(userRepository).save(user);
    }

    @Test
    void loginRejectsInactiveAccount() {
        User user = activeUser();
        user.setAccountStatus(AccountStatus.INACTIVE);
        when(userRepository.findByUsernameOrEmail("jdoe")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("jdoe", "secret"), "ip", "ua"))
                .isInstanceOf(DisabledException.class);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void loginRejectsLockedAccount() {
        User user = activeUser();
        user.setAccountLockedUntil(Instant.now().plus(10, ChronoUnit.MINUTES));
        when(userRepository.findByUsernameOrEmail("jdoe")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("jdoe", "secret"), "ip", "ua"))
                .isInstanceOf(LockedException.class);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void accountLocksAfterMaxFailedAttempts() {
        User user = activeUser();
        user.setFailedLoginAttempts(lockout.getMaxFailedAttempts() - 1);
        when(userRepository.findByUsernameOrEmail("jdoe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "$2a$hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("jdoe", "bad"), "ip", "ua"))
                .isInstanceOf(LockedException.class);

        assertThat(user.getAccountLockedUntil()).isAfter(Instant.now());
    }

    @Test
    void refreshRotatesToken() {
        User user = activeUser();
        RefreshToken stored = new RefreshToken();
        stored.setUser(user);
        stored.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
        when(refreshTokenService.requireActive("raw.old")).thenReturn(stored);
        when(refreshTokenService.issue(eq(user), any(), any()))
                .thenReturn(new IssuedRefreshToken("raw.new", 20L));
        when(jwtService.generateAccessToken(user)).thenReturn("new.access");
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);

        AuthResponse response = authService.refresh("raw.old", "ip", "ua");

        assertThat(response.accessToken()).isEqualTo("new.access");
        assertThat(response.refreshToken()).isEqualTo("raw.new");
        assertThat(stored.isRevoked()).isTrue();
        assertThat(stored.getReplacedById()).isEqualTo(20L);
    }

    @Test
    void refreshRejectsInvalidToken() {
        when(refreshTokenService.requireActive("bad"))
                .thenThrow(new InvalidTokenException("Invalid refresh token"));

        assertThatThrownBy(() -> authService.refresh("bad", "ip", "ua"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void refreshRejectsInactiveUser() {
        User user = activeUser();
        user.setAccountStatus(AccountStatus.INACTIVE);
        RefreshToken stored = new RefreshToken();
        stored.setUser(user);
        stored.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
        when(refreshTokenService.requireActive("raw.old")).thenReturn(stored);

        assertThatThrownBy(() -> authService.refresh("raw.old", "ip", "ua"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void logoutRevokesRefreshToken() {
        authService.logout("raw.token");
        verify(refreshTokenService).revoke("raw.token");
    }

    @Test
    void changePasswordFailsOnWrongCurrentPassword() {
        User user = activeUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "$2a$hash")).thenReturn(false);

        assertThatThrownBy(() ->
                authService.changePassword(1L, new ChangePasswordRequest("wrong", "NewPass123")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void changePasswordSucceedsAndRevokesSessions() {
        User user = activeUser();
        user.setMustChangePassword(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "$2a$hash")).thenReturn(true);
        when(passwordEncoder.encode("NewPass123")).thenReturn("$2a$newhash");

        authService.changePassword(1L, new ChangePasswordRequest("old", "NewPass123"));

        assertThat(user.getPasswordHash()).isEqualTo("$2a$newhash");
        assertThat(user.isMustChangePassword()).isFalse();
        verify(refreshTokenService).revokeAllForUser(1L);
    }

    @Test
    void resetPasswordFailsForInvalidToken() {
        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                authService.resetPassword(new ResetPasswordRequest("nope", "NewPass123")))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void resetPasswordFailsForExpiredToken() {
        PasswordResetToken token = new PasswordResetToken();
        token.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        when(passwordResetTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() ->
                authService.resetPassword(new ResetPasswordRequest("expired", "NewPass123")))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void resetPasswordSucceeds() {
        User user = activeUser();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        when(passwordResetTokenRepository.findByTokenHash(TokenHashUtil.sha256("good")))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewPass123")).thenReturn("$2a$reset");

        authService.resetPassword(new ResetPasswordRequest("good", "NewPass123"));

        assertThat(user.getPasswordHash()).isEqualTo("$2a$reset");
        assertThat(token.getUsedAt()).isNotNull();
        verify(refreshTokenService).revokeAllForUser(1L);
    }

    @Test
    void forgotPasswordNeverRevealsWhetherAccountExists() {
        when(userRepository.findByUsernameOrEmail("ghost")).thenReturn(Optional.empty());

        var response = authService.forgotPassword(
                new com.pcc.identity.dto.request.ForgotPasswordRequest("ghost"));

        assertThat(response.message()).containsIgnoringCase("if the account exists");
        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void loginFailureRecordsHistory() {
        User user = activeUser();
        when(userRepository.findByUsernameOrEmail("jdoe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "$2a$hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("jdoe", "bad"), "ip", "ua"));

        ArgumentCaptor<com.pcc.identity.enums.LoginEventType> captor =
                ArgumentCaptor.forClass(com.pcc.identity.enums.LoginEventType.class);
        verify(loginHistoryService).record(eq(1L), eq("jdoe"), captor.capture(), eq("ip"), eq("ua"), anyString());
        assertThat(captor.getValue()).isEqualTo(com.pcc.identity.enums.LoginEventType.LOGIN_FAILED);
    }
}
