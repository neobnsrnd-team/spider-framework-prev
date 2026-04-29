package com.example.spider_admin.global.security;

import com.example.spider_admin.domain.user.enums.UserState;
import com.example.spider_admin.global.security.dto.AuthenticatedUser;
import java.util.Collection;
import java.util.Set;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class CustomUserDetails implements UserDetails {

    private final AuthenticatedUser user;
    private final Set<GrantedAuthority> authorities;

    public CustomUserDetails(AuthenticatedUser user, Set<GrantedAuthority> authorities) {
        this.user = user;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUserId();
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

    @Override
    public boolean isEnabled() {
        return UserState.NORMAL == user.getUserState();
    }

    public String getUserId() {
        return user.getUserId();
    }

    public String getDisplayName() {
        return user.getUserName();
    }

    public String getRoleId() {
        return user.getRoleId();
    }
}
