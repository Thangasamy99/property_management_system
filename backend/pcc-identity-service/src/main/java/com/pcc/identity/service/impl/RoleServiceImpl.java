package com.pcc.identity.service.impl;

import com.pcc.identity.audit.AuditService;
import com.pcc.identity.dto.request.RolePermissionsUpdateRequest;
import com.pcc.identity.dto.request.RoleRequest;
import com.pcc.identity.dto.response.PageResponse;
import com.pcc.identity.dto.response.RoleResponse;
import com.pcc.identity.dto.response.UserResponse;
import com.pcc.identity.entity.Permission;
import com.pcc.identity.entity.Role;
import com.pcc.identity.exception.DuplicateResourceException;
import com.pcc.identity.exception.ResourceNotFoundException;
import com.pcc.identity.mapper.RoleMapper;
import com.pcc.identity.mapper.UserMapper;
import com.pcc.identity.repository.PermissionRepository;
import com.pcc.identity.repository.RoleRepository;
import com.pcc.identity.repository.UserRepository;
import com.pcc.identity.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public RoleResponse create(RoleRequest request) {
        String code = request.code().trim().toUpperCase();
        if (roleRepository.existsByCode(code)) {
            throw new DuplicateResourceException("Role code already exists: " + code);
        }
        Role role = new Role();
        role.setCode(code);
        role.setName(request.name().trim());
        role.setDescription(request.description());
        Role saved = roleRepository.save(role);
        auditService.record("ROLE_CREATE", "ROLE", String.valueOf(saved.getId()), "Created role " + code);
        return RoleMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RoleResponse update(Long id, RoleRequest request) {
        Role role = requireRole(id);
        role.setName(request.name().trim());
        role.setDescription(request.description());
        Role saved = roleRepository.save(role);
        auditService.record("ROLE_UPDATE", "ROLE", String.valueOf(id), "Updated role " + saved.getCode());
        return RoleMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getById(Long id) {
        return RoleMapper.toResponse(requireRole(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> list() {
        return roleRepository.findAll().stream().map(RoleMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getPermissions(Long id) {
        return requireRole(id).getPermissions().stream()
                .map(Permission::getCode)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public RoleResponse updatePermissions(Long id, RolePermissionsUpdateRequest request) {
        Role role = requireRole(id);

        Set<Permission> permissions = new HashSet<>();
        List<String> missing = new ArrayList<>();
        for (String code : request.permissionCodes()) {
            permissionRepository.findByCode(code.trim().toUpperCase())
                    .ifPresentOrElse(permissions::add, () -> missing.add(code));
        }
        if (!missing.isEmpty()) {
            throw new ResourceNotFoundException("Unknown permission codes: " + missing);
        }

        role.setPermissions(permissions);
        Role saved = roleRepository.save(role);
        auditService.record("ROLE_ASSIGN_PERMISSION", "ROLE", String.valueOf(id),
                "Permissions set to " + permissions.stream().map(Permission::getCode).sorted().toList());
        return RoleMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(Long id, Pageable pageable) {
        requireRole(id);
        return PageResponse.from(userRepository.findByRoles_Id(id, pageable), UserMapper::toResponse);
    }

    private Role requireRole(Long id) {
        return roleRepository.findWithPermissionsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));
    }
}
