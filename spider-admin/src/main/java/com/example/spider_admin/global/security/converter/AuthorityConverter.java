package com.example.spider_admin.global.security.converter;

import com.example.spider_admin.global.security.config.MenuResourcePermissions;
import com.example.spider_admin.global.security.constant.MenuAccessLevel;
import com.example.spider_admin.global.security.dto.MenuPermission;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthorityConverter {

    private final MenuResourcePermissions menuResourcePermissions;

    public Set<GrantedAuthority> convert(List<MenuPermission> permissions) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        for (MenuPermission p : permissions) {
            MenuAccessLevel level = MenuAccessLevel.fromCode(p.getAuthCode());
            if (level == null) {
                continue;
            }
            for (String res : menuResourcePermissions.getDerivedResourceAuthorities(p.getMenuId(), level)) {
                authorities.add(new SimpleGrantedAuthority(res));
            }
        }
        return Collections.unmodifiableSet(authorities);
    }
}
