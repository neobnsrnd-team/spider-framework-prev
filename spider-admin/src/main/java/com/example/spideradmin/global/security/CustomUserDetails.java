package com.example.spideradmin.global.security;

import com.example.spideradmin.domain.user.enums.UserState;
import com.example.spideradmin.global.security.dto.AuthenticatedUser;
import java.util.Collection;
import java.util.Set;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class CustomUserDetails implements UserDetails {

    /** CmsRedirectController.isCmsAdmin() 과 동일한 역할 판별 기준 */
    private static final String ADMIN_ROLE = "ADMIN";

    private static final String CMS_ADMIN_ROLE = "cms_admin";

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

    /** ADMIN 또는 cms_admin 역할이면 CMS 관리자로 판별 — CmsRedirectController.isCmsAdmin() 과 동일 기준 */
    public boolean isCmsAdmin() {
        String roleId = user.getRoleId();
        return ADMIN_ROLE.equals(roleId) || CMS_ADMIN_ROLE.equals(roleId);
    }
}
