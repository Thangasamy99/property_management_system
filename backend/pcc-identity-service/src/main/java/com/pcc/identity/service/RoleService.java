package com.pcc.identity.service;

import com.pcc.identity.dto.request.RolePermissionsUpdateRequest;
import com.pcc.identity.dto.request.RoleRequest;
import com.pcc.identity.dto.response.PageResponse;
import com.pcc.identity.dto.response.RoleResponse;
import com.pcc.identity.dto.response.UserResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public interface RoleService {

    RoleResponse create(RoleRequest request);

    RoleResponse update(Long id, RoleRequest request);

    RoleResponse getById(Long id);

    List<RoleResponse> list();

    Set<String> getPermissions(Long id);

    RoleResponse updatePermissions(Long id, RolePermissionsUpdateRequest request);

    PageResponse<UserResponse> listUsers(Long id, Pageable pageable);
}
