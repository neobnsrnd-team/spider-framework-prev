package com.example.spider_admin.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.spider_admin.domain.user.enums.UserState;
import com.example.spider_admin.global.security.dto.AuthenticatedUser;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@DisplayName("CustomUserDetails 테스트")
class CustomUserDetailsTest {

    @Test
    @DisplayName("정상 사용자는 isEnabled가 true이다")
    void isEnabled_normalUser_returnsTrue() {
        CustomUserDetails details = createDetails(UserState.NORMAL);

        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("삭제된 사용자는 isEnabled가 false이다")
    void isEnabled_deletedUser_returnsFalse() {
        CustomUserDetails details = createDetails(UserState.DELETED);

        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("정지된 사용자는 isEnabled가 false이다")
    void isEnabled_suspendedUser_returnsFalse() {
        CustomUserDetails details = createDetails(UserState.SUSPENDED);

        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("getUsername은 userId를 반환한다")
    void getUsername_returnsUserId() {
        CustomUserDetails details = createDetails(UserState.NORMAL);

        assertThat(details.getUsername()).isEqualTo("testUser");
    }

    @Test
    @DisplayName("getPassword는 비밀번호를 반환한다")
    void getPassword_returnsPassword() {
        CustomUserDetails details = createDetails(UserState.NORMAL);

        assertThat(details.getPassword()).isEqualTo("encodedPassword");
    }

    @Test
    @DisplayName("getUserId는 userId를 반환한다")
    void getUserId_returnsUserId() {
        CustomUserDetails details = createDetails(UserState.NORMAL);

        assertThat(details.getUserId()).isEqualTo("testUser");
    }

    @Test
    @DisplayName("getRoleId는 roleId를 반환한다")
    void getRoleId_returnsRoleId() {
        CustomUserDetails details = createDetails(UserState.NORMAL);

        assertThat(details.getRoleId()).isEqualTo("admin");
    }

    @Test
    @DisplayName("getAuthorities는 생성자로 전달된 권한을 반환한다")
    void getAuthorities_returnsProvidedAuthorities() {
        Set<GrantedAuthority> authorities =
                Set.of(new SimpleGrantedAuthority("USER:R"), new SimpleGrantedAuthority("USER:W"));
        AuthenticatedUser user = new AuthenticatedUser("testUser", "관리자", "admin", "encodedPassword", UserState.NORMAL);
        CustomUserDetails details = new CustomUserDetails(user, authorities);

        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("USER:R", "USER:W");
    }

    @Test
    @DisplayName("계정 만료/잠금/자격증명 만료는 항상 true이다")
    void accountStatusMethods_alwaysTrue() {
        CustomUserDetails details = createDetails(UserState.NORMAL);

        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
    }

    private CustomUserDetails createDetails(UserState state) {
        AuthenticatedUser user = new AuthenticatedUser("testUser", "관리자", "admin", "encodedPassword", state);
        Set<GrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("USER:R"));
        return new CustomUserDetails(user, authorities);
    }
}
