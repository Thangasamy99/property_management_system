package com.pcc.identity.service;

import com.pcc.identity.dto.request.UserCreateRequest;
import com.pcc.identity.dto.request.UserRolesUpdateRequest;
import com.pcc.identity.dto.request.UserStatusUpdateRequest;
import com.pcc.identity.dto.request.UserUpdateRequest;
import com.pcc.identity.dto.response.PageResponse;
import com.pcc.identity.dto.response.UserCreatedResponse;
import com.pcc.identity.dto.response.UserResponse;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserCreatedResponse create(UserCreateRequest request);

    UserResponse update(Long id, UserUpdateRequest request);

    UserResponse getById(Long id);

    PageResponse<UserResponse> list(String search, Pageable pageable);

    UserResponse updateStatus(Long id, UserStatusUpdateRequest request);

    UserResponse updateRoles(Long id, UserRolesUpdateRequest request);

    /** Admin-side reset: sets a temporary password and forces change at next login. */
    String resetPassword(Long id);
}
