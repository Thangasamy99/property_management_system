package com.pcc.document.security;

import io.jsonwebtoken.Claims;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Principal reconstructed entirely from JWT claims — no local user store. */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String employeeCode;
    private final Set<String> roleCodes;
    private final Collection<? extends GrantedAuthority> authorities;

    private UserPrincipal(Claims claims) {
        this.id = Long.parseLong(claims.getSubject());
        this.username = claims.get("username", String.class);
        this.employeeCode = claims.get("employeeCode", String.class);
        this.roleCodes = new HashSet<>(claims.get("roles", List.class));
        Set<GrantedAuthority> auths = new HashSet<>();
        roleCodes.forEach(r -> auths.add(new SimpleGrantedAuthority("ROLE_" + r)));
        List<String> permissions = claims.get("permissions", List.class);
        if (permissions != null) {
            permissions.forEach(p -> auths.add(new SimpleGrantedAuthority(p)));
        }
        this.authorities = auths;
    }

    public static UserPrincipal from(Claims claims) {
        return new UserPrincipal(claims);
    }

    public boolean hasRole(String roleCode) {
        return roleCodes.contains(roleCode);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }
}
