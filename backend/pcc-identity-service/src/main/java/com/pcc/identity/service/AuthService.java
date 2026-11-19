package com.pcc.identity.service;

import com.pcc.identity.dto.request.ChangePasswordRequest;
import com.pcc.identity.dto.request.ForgotPasswordRequest;
import com.pcc.identity.dto.request.LoginRequest;
import com.pcc.identity.dto.request.ResetPasswordRequest;
import com.pcc.identity.dto.response.AuthResponse;
import com.pcc.identity.dto.response.LoginHistoryResponse;
import com.pcc.identity.dto.response.MessageResponse;
import com.pcc.identity.dto.response.PageResponse;
import com.pcc.identity.dto.response.UserResponse;
import org.springframework.data.domain.Pageable;

public interface AuthService {

    AuthResponse login(LoginRequest request, String ipAddress, String userAgent);

    AuthResponse refresh(String rawRefreshToken, String ipAddress, String userAgent);

    void logout(String rawRefreshToken);

    UserResponse currentUser(Long userId);

    MessageResponse changePassword(Long userId, ChangePasswordRequest request);

    MessageResponse forgotPassword(ForgotPasswordRequest request);

    MessageResponse resetPassword(ResetPasswordRequest request);

    PageResponse<LoginHistoryResponse> loginHistory(Long userId, Pageable pageable);
}
