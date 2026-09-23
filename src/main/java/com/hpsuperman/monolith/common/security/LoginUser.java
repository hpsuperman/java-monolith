package com.hpsuperman.monolith.common.security;

import com.hpsuperman.monolith.common.enums.EnabledStatus;
import io.jsonwebtoken.Claims;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
@ToString(exclude = "password")
public class LoginUser implements UserDetails, Serializable {
    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String username;

    private final String password;
    private final String nickname;
    private final EnabledStatus status;
    private final Set<String> roles;

    private final List<GrantedAuthority> authorities;

    public LoginUser(Long userId, String username, String password,
                     String nickname, EnabledStatus status, Collection<String> roles) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.status = status;
        this.roles = roles == null ? Set.of() : new LinkedHashSet<>(roles);
        this.authorities = this.roles.stream()

                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(
                        role.startsWith("ROLE_") ? role : "ROLE_" + role))
                .toList();
    }

    public static LoginUser fromClaims(Claims claims) {
        String username = claims.get(JwtTokenProvider.CLAIM_USERNAME, String.class);
        String nickname = claims.get(JwtTokenProvider.CLAIM_NICKNAME, String.class);

        List<?> rawRoles = claims.get(JwtTokenProvider.CLAIM_ROLES, List.class);
        Set<String> roles = new LinkedHashSet<>();
        if (rawRoles != null) {
            rawRoles.forEach(r -> roles.add(String.valueOf(r)));
        }

        return new LoginUser(Long.valueOf(claims.getSubject()), username, null, nickname,
                EnabledStatus.ENABLED, roles);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isEnabled() {
        return status == EnabledStatus.ENABLED;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
