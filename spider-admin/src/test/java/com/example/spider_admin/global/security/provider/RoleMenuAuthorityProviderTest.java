package com.example.spider_admin.global.security.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.spider_admin.global.security.converter.AuthorityConverter;
import com.example.spider_admin.global.security.dto.MenuPermission;
import com.example.spider_admin.global.security.mapper.AuthorityMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleMenuAuthorityProvider 테스트")
class RoleMenuAuthorityProviderTest {

    @Mock
    private AuthorityMapper authorityMapper;

    @Mock
    private AuthorityConverter authorityConverter;

    @InjectMocks
    private RoleMenuAuthorityProvider provider;

    @Test
    @DisplayName("roleId 기반으로 메뉴 권한을 조회하고 변환한다")
    void getAuthorities_queriesByRoleId() {
        // given
        String userId = "testUser";
        String roleId = "admin";
        List<MenuPermission> permissions = List.of(new MenuPermission("USER", "W"));
        Set<GrantedAuthority> expected =
                Set.of(new SimpleGrantedAuthority("USER:R"), new SimpleGrantedAuthority("USER:W"));

        when(authorityMapper.selectMenuPermissionsByRoleId(roleId)).thenReturn(permissions);
        when(authorityConverter.convert(permissions)).thenReturn(expected);

        // when
        Set<GrantedAuthority> result = provider.getAuthorities(userId, roleId);

        // then
        assertThat(result).isEqualTo(expected);
        verify(authorityMapper).selectMenuPermissionsByRoleId(roleId);
        verify(authorityConverter).convert(permissions);
    }
}
