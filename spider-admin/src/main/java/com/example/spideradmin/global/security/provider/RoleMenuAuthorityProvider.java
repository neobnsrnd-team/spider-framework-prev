package com.example.spideradmin.global.security.provider;

import com.example.spideradmin.global.security.converter.AuthorityConverter;
import com.example.spideradmin.global.security.mapper.AuthorityMapper;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.security.authority-source", havingValue = "role-menu")
@RequiredArgsConstructor
public class RoleMenuAuthorityProvider implements AuthorityProvider {

    private final AuthorityMapper authorityMapper;
    private final AuthorityConverter authorityConverter;

    @Override
    public Set<GrantedAuthority> getAuthorities(String userId, String roleId) {
        return authorityConverter.convert(authorityMapper.selectMenuPermissionsByRoleId(roleId));
    }
}
