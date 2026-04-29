package com.example.spider_admin.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.spider_admin.domain.user.mapper.UserMapper;
import com.example.spider_admin.global.security.dto.UserAuthInfo;
import com.example.spider_admin.global.security.provider.AuthorityProvider;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService 테스트")
class CustomUserDetailsServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private AuthorityProvider authorityProvider;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    @DisplayName("존재하는 사용자를 로드하면 CustomUserDetails를 반환한다")
    void loadUserByUsername_existingUser_returnsUserDetails() {
        // given
        String userId = "testUser";
        UserAuthInfo authInfo = new UserAuthInfo(userId, "관리자", "admin", "encodedPw", "1");
        Set<GrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("USER:R"));

        when(userMapper.selectAuthInfoById(userId)).thenReturn(authInfo);
        when(authorityProvider.getAuthorities(userId, "admin")).thenReturn(authorities);

        // when
        CustomUserDetails result = (CustomUserDetails) service.loadUserByUsername(userId);

        // then
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getRoleId()).isEqualTo("admin");
        assertThat(result.getPassword()).isEqualTo("encodedPw");
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("USER:R");
    }

    @Test
    @DisplayName("존재하지 않는 사용자를 로드하면 UsernameNotFoundException을 던진다")
    void loadUserByUsername_nonExistingUser_throwsException() {
        // given
        String userId = "unknown";
        when(userMapper.selectAuthInfoById(userId)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> service.loadUserByUsername(userId))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage(userId);
    }
}
