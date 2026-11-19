package com.pcc.identity.security;

import com.pcc.identity.entity.Permission;
import com.pcc.identity.entity.User;
import com.pcc.identity.enums.AccountStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final boolean enabled;
    private final boolean locked;
    private final Set<String> roleCodes;
    private final Collection<? extends GrantedAuthority> authorities;

    private UserPrincipal(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPasswordHash();
        this.enabled = user.getAccountStatus() == AccountStatus.ACTIVE;
        this.locked = user.isLocked();
        this.roleCodes = new HashSet<>();
        Set<GrantedAuthority> auths = new HashSet<>();
        user.getRoles().forEach(role -> {
            roleCodes.add(role.getCode());
            auths.add(new SimpleGrantedAuthority("ROLE_" + role.getCode()));
            role.getPermissions().forEach(p -> auths.add(new SimpleGrantedAuthority(p.getCode())));
        });
        this.authorities = auths;
    }

    public static UserPrincipal from(User user) {
        return new UserPrincipal(user);
    }

    public boolean hasRole(String roleCode) {
        return roleCodes.contains(roleCode);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
