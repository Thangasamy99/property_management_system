package com.pcc.identity.service.impl;

import com.pcc.identity.audit.AuditService;
import com.pcc.identity.dto.request.UserCreateRequest;
import com.pcc.identity.dto.request.UserRolesUpdateRequest;
import com.pcc.identity.dto.request.UserStatusUpdateRequest;
import com.pcc.identity.dto.request.UserUpdateRequest;
import com.pcc.identity.dto.response.PageResponse;
import com.pcc.identity.dto.response.UserCreatedResponse;
import com.pcc.identity.dto.response.UserResponse;
import com.pcc.identity.entity.Role;
import com.pcc.identity.entity.User;
import com.pcc.identity.enums.AccountStatus;
import com.pcc.identity.exception.DuplicateResourceException;
import com.pcc.identity.exception.ForbiddenOperationException;
import com.pcc.identity.exception.ResourceNotFoundException;
import com.pcc.identity.mapper.UserMapper;
import com.pcc.identity.repository.RoleRepository;
import com.pcc.identity.repository.UserRepository;
import com.pcc.identity.service.RefreshTokenService;
import com.pcc.identity.service.UserService;
import com.pcc.identity.util.PasswordGenerator;
import com.pcc.identity.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String ADMIN_ROLE = "ADMIN";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;

    @Override
    @Transactional
    public UserCreatedResponse create(UserCreateRequest request) {
        assertUnique(request.username(), request.email(), request.employeeCode(), null);

        User user = new User();
        user.setEmployeeCode(request.employeeCode().trim());
        user.setUsername(request.username().trim());
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPhone(request.phone());
        user.setAccountStatus(AccountStatus.ACTIVE);

        String temporaryPassword = null;
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            user.setMustChangePassword(false);
        } else {
            temporaryPassword = PasswordGenerator.temporaryPassword();
            user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
            user.setMustChangePassword(true);
        }

        user.setRoles(resolveRoles(request.roleCodes()));
        user.setCreatedBy(SecurityUtils.currentUsername());

        User saved = userRepository.save(user);
        auditService.record("USER_CREATE", "USER", String.valueOf(saved.getId()),
                "Created employee " + saved.getUsername());
        return new UserCreatedResponse(UserMapper.toResponse(saved), temporaryPassword);
    }

    @Override
    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = requireUser(id);
        assertUnique(request.username(), request.email(), request.employeeCode(), id);

        user.setEmployeeCode(request.employeeCode().trim());
        user.setUsername(request.username().trim());
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPhone(request.phone());

        User saved = userRepository.save(user);
        auditService.record("USER_UPDATE", "USER", String.valueOf(id), "Updated employee " + saved.getUsername());
        return UserMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return UserMapper.toResponse(requireUser(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(String search, Pageable pageable) {
        String q = (search == null || search.isBlank())
                ? null
                : "%" + search.trim().toLowerCase() + "%";
        return PageResponse.from(userRepository.search(q, pageable), UserMapper::toResponse);
    }

    @Override
    @Transactional
    public UserResponse updateStatus(Long id, UserStatusUpdateRequest request) {
        User user = requireUser(id);

        if (Objects.equals(SecurityUtils.currentUserId(), id)
                && request.status() == AccountStatus.INACTIVE) {
            throw new ForbiddenOperationException("You cannot deactivate your own account");
        }

        user.setAccountStatus(request.status());
        if (request.status() == AccountStatus.INACTIVE) {
            refreshTokenService.revokeAllForUser(id);
        }

        User saved = userRepository.save(user);
        auditService.record("USER_STATUS_" + request.status(), "USER", String.valueOf(id),
                "Account status set to " + request.status());
        return UserMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse updateRoles(Long id, UserRolesUpdateRequest request) {
        User user = requireUser(id);
        Set<Role> roles = resolveRoles(request.roleCodes());

        // Self-elevation guard: a user may not grant themselves the ADMIN role.
        boolean isSelf = Objects.equals(SecurityUtils.currentUserId(), id);
        boolean alreadyAdmin = UserMapper.hasRole(user, ADMIN_ROLE);
        boolean addingAdmin = roles.stream().anyMatch(r -> ADMIN_ROLE.equals(r.getCode()));
        if (isSelf && addingAdmin && !alreadyAdmin) {
            throw new ForbiddenOperationException("You cannot assign yourself the ADMIN role");
        }

        user.setRoles(roles);
        User saved = userRepository.save(user);
        auditService.record("USER_ASSIGN_ROLE", "USER", String.valueOf(id),
                "Roles set to " + roles.stream().map(Role::getCode).sorted().toList());
        return UserMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public String resetPassword(Long id) {
        User user = requireUser(id);
        String temporaryPassword = PasswordGenerator.temporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(id);
        auditService.record("USER_PASSWORD_RESET", "USER", String.valueOf(id),
                "Admin reset password for " + user.getUsername());
        return temporaryPassword;
    }

    private User requireUser(Long id) {
        return userRepository.findWithRolesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private Set<Role> resolveRoles(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return new HashSet<>();
        }
        Set<Role> roles = new HashSet<>();
        List<String> missing = new ArrayList<>();
        for (String code : roleCodes) {
            roleRepository.findByCode(code.trim().toUpperCase())
                    .ifPresentOrElse(roles::add, () -> missing.add(code));
        }
        if (!missing.isEmpty()) {
            throw new ResourceNotFoundException("Unknown role codes: " + missing);
        }
        return roles;
    }

    private void assertUnique(String username, String email, String employeeCode, Long excludeId) {
        check(userRepository.findByUsername(username.trim()), excludeId, "Username", username.trim());
        check(userRepository.findByEmail(email.trim().toLowerCase()), excludeId, "Email", email.trim());
        check(userRepository.findByEmployeeCode(employeeCode.trim()), excludeId, "Employee code", employeeCode.trim());
    }

    private void check(Optional<User> existing, Long excludeId, String field, String value) {
        existing.filter(u -> excludeId == null || !u.getId().equals(excludeId))
                .ifPresent(u -> {
                    throw new DuplicateResourceException(field + " already in use: " + value);
                });
    }
}
