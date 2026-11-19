package com.pcc.identity.service;

import com.pcc.identity.audit.AuditService;
import com.pcc.identity.dto.request.UserCreateRequest;
import com.pcc.identity.dto.request.UserRolesUpdateRequest;
import com.pcc.identity.dto.request.UserStatusUpdateRequest;
import com.pcc.identity.dto.response.UserCreatedResponse;
import com.pcc.identity.dto.response.UserResponse;
import com.pcc.identity.entity.Role;
import com.pcc.identity.entity.User;
import com.pcc.identity.enums.AccountStatus;
import com.pcc.identity.exception.DuplicateResourceException;
import com.pcc.identity.exception.ForbiddenOperationException;
import com.pcc.identity.exception.ResourceNotFoundException;
import com.pcc.identity.repository.RoleRepository;
import com.pcc.identity.repository.UserRepository;
import com.pcc.identity.security.UserPrincipal;
import com.pcc.identity.service.impl.UserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock RefreshTokenService refreshTokenService;
    @Mock AuditService auditService;

    @InjectMocks
    UserServiceImpl userService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private UserCreateRequest createRequest() {
        return new UserCreateRequest("PCC-100", "jane", "Jane", "Doe",
                "jane@pcc.cm", "+237600000000", null, Set.of("MANAGER"));
    }

    private Role role(String code) {
        Role role = new Role();
        role.setId(code.equals("ADMIN") ? 1L : 2L);
        role.setCode(code);
        return role;
    }

    private void authenticateAs(User user) {
        UserPrincipal principal = UserPrincipal.from(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    void createGeneratesTemporaryPasswordAndAssignsRoles() {
        when(roleRepository.findByCode("MANAGER")).thenReturn(Optional.of(role("MANAGER")));
        when(passwordEncoder.encode(any())).thenReturn("$2a$hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(5L);
            return u;
        });

        UserCreatedResponse result = userService.create(createRequest());

        assertThat(result.user().username()).isEqualTo("jane");
        assertThat(result.user().roles()).extracting("code").containsExactly("MANAGER");
        assertThat(result.temporaryPassword()).isNotBlank();
        assertThat(result.user().mustChangePassword()).isTrue();
        verify(auditService).record(eq("USER_CREATE"), eq("USER"), eq("5"), any());
    }

    @Test
    void createRejectsDuplicateUsername() {
        User existing = new User();
        existing.setId(99L);
        existing.setUsername("jane");
        when(userRepository.findByUsername("jane")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userService.create(createRequest()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username");
        verify(userRepository, never()).save(any());
    }

    @Test
    void createRejectsDuplicateEmail() {
        User existing = new User();
        existing.setId(99L);
        existing.setEmail("jane@pcc.cm");
        when(userRepository.findByEmail("jane@pcc.cm")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userService.create(createRequest()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email");
    }

    @Test
    void createRejectsUnknownRole() {
        when(roleRepository.findByCode("MANAGER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.create(createRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("MANAGER");
    }

    @Test
    void updateRolesAssignsNewRoles() {
        User target = new User();
        target.setId(7L);
        target.setUsername("target");
        when(userRepository.findWithRolesById(7L)).thenReturn(Optional.of(target));
        when(roleRepository.findByCode("ACCOUNTANT")).thenReturn(Optional.of(role("ACCOUNTANT")));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.updateRoles(7L, new UserRolesUpdateRequest(Set.of("ACCOUNTANT")));

        assertThat(response.roles()).extracting("code").containsExactly("ACCOUNTANT");
        verify(auditService).record(eq("USER_ASSIGN_ROLE"), eq("USER"), eq("7"), any());
    }

    @Test
    void cannotGrantSelfAdminRole() {
        User me = new User();
        me.setId(7L);
        me.setUsername("me");
        me.setRoles(Set.of(role("MANAGER")));
        when(userRepository.findWithRolesById(7L)).thenReturn(Optional.of(me));
        when(roleRepository.findByCode("ADMIN")).thenReturn(Optional.of(role("ADMIN")));
        when(roleRepository.findByCode("MANAGER")).thenReturn(Optional.of(role("MANAGER")));
        authenticateAs(me);

        assertThatThrownBy(() ->
                userService.updateRoles(7L, new UserRolesUpdateRequest(Set.of("ADMIN", "MANAGER"))))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void adminCanGrantAdminToAnotherUser() {
        User actor = new User();
        actor.setId(1L);
        actor.setUsername("boss");
        actor.setRoles(Set.of(role("ADMIN")));

        User target = new User();
        target.setId(7L);
        target.setUsername("target");
        when(userRepository.findWithRolesById(7L)).thenReturn(Optional.of(target));
        when(roleRepository.findByCode("ADMIN")).thenReturn(Optional.of(role("ADMIN")));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        authenticateAs(actor);

        UserResponse response = userService.updateRoles(7L, new UserRolesUpdateRequest(Set.of("ADMIN")));

        assertThat(response.roles()).extracting("code").containsExactly("ADMIN");
    }

    @Test
    void cannotDeactivateSelf() {
        User me = new User();
        me.setId(7L);
        me.setUsername("me");
        when(userRepository.findWithRolesById(7L)).thenReturn(Optional.of(me));
        authenticateAs(me);

        assertThatThrownBy(() ->
                userService.updateStatus(7L, new UserStatusUpdateRequest(AccountStatus.INACTIVE)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void deactivateRevokesSessions() {
        User target = new User();
        target.setId(7L);
        target.setUsername("target");
        when(userRepository.findWithRolesById(7L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.updateStatus(7L, new UserStatusUpdateRequest(AccountStatus.INACTIVE));

        assertThat(response.accountStatus()).isEqualTo(AccountStatus.INACTIVE);
        verify(refreshTokenService).revokeAllForUser(7L);
    }

    @Test
    void resetPasswordForcesChangeAndRevokesSessions() {
        User target = new User();
        target.setId(7L);
        target.setUsername("target");
        when(userRepository.findWithRolesById(7L)).thenReturn(Optional.of(target));
        when(passwordEncoder.encode(any())).thenReturn("$2a$new");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        String temp = userService.resetPassword(7L);

        assertThat(temp).isNotBlank();
        assertThat(target.isMustChangePassword()).isTrue();
        verify(refreshTokenService).revokeAllForUser(7L);
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(userRepository.findWithRolesById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
