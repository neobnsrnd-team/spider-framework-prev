package com.example.spider_admin.global.security.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.spider_admin.global.security.config.MenuResourcePermissions;
import com.example.spider_admin.global.security.dto.MenuPermission;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

@DisplayName("AuthorityConverter 테스트")
class AuthorityConverterTest {

    private AuthorityConverter converter;

    @BeforeEach
    void setUp() {
        MenuResourcePermissions permissions = new MenuResourcePermissions();
        permissions.setPermissions(Map.of(
                "USER", Map.of("R", "USER:R", "W", "USER:W"),
                "ROLE", Map.of("R", "ROLE:R, MENU:R", "W", "ROLE:W, MENU:R"),
                "ORG_CODE", Map.of("R", "ORG_CODE:R, ORG:R, CODE_GROUP:R", "W", "ORG_CODE:W, ORG:R, CODE_GROUP:R"),
                "APP_MAPPING",
                        Map.of(
                                "R",
                                "APP_MAPPING:R, ORG:R, GATEWAY_MANAGEMENT:R, TRX:R",
                                "W",
                                "APP_MAPPING:W, ORG:R, GATEWAY_MANAGEMENT:R, TRX:R")));
        converter = new AuthorityConverter(permissions);
    }

    @Test
    @DisplayName("READ 권한의 MenuPermission을 R 리소스로 변환한다")
    void convert_readPermission_returnsReadAuthorities() {
        List<MenuPermission> permissions = List.of(new MenuPermission("USER", "R"));

        Set<GrantedAuthority> result = converter.convert(permissions);

        assertThat(authorityStrings(result)).containsExactly("USER:R");
    }

    @Test
    @DisplayName("WRITE 권한의 MenuPermission을 R+W 리소스로 변환한다")
    void convert_writePermission_returnsReadAndWriteAuthorities() {
        List<MenuPermission> permissions = List.of(new MenuPermission("USER", "W"));

        Set<GrantedAuthority> result = converter.convert(permissions);

        assertThat(authorityStrings(result)).containsExactlyInAnyOrder("USER:R", "USER:W");
    }

    @Test
    @DisplayName("여러 메뉴의 권한을 합산하여 변환한다")
    void convert_multiplePermissions_mergesAuthorities() {
        List<MenuPermission> permissions = List.of(new MenuPermission("USER", "R"), new MenuPermission("ROLE", "W"));

        Set<GrantedAuthority> result = converter.convert(permissions);

        assertThat(authorityStrings(result)).containsExactlyInAnyOrder("USER:R", "ROLE:R", "MENU:R", "ROLE:W");
    }

    @Test
    @DisplayName("유효하지 않은 authCode는 무시한다")
    void convert_invalidAuthCode_isSkipped() {
        List<MenuPermission> permissions = List.of(new MenuPermission("USER", "X"));

        Set<GrantedAuthority> result = converter.convert(permissions);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("빈 리스트를 변환하면 빈 Set을 반환한다")
    void convert_emptyList_returnsEmpty() {
        Set<GrantedAuthority> result = converter.convert(List.of());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("변환 결과는 불변 Set이다")
    void convert_resultIsUnmodifiable() {
        Set<GrantedAuthority> result = converter.convert(List.of(new MenuPermission("USER", "R")));

        assertThat(result).isUnmodifiable();
    }

    @Test
    @DisplayName("교차 도메인: APP_MAPPING R 권한은 ORG:R, GATEWAY_MANAGEMENT:R, TRX:R을 포함한다")
    void convert_crossDomain_appMappingRead_includesDependencies() {
        List<MenuPermission> permissions = List.of(new MenuPermission("APP_MAPPING", "R"));

        Set<GrantedAuthority> result = converter.convert(permissions);

        assertThat(authorityStrings(result))
                .containsExactlyInAnyOrder("APP_MAPPING:R", "ORG:R", "GATEWAY_MANAGEMENT:R", "TRX:R");
    }

    @Test
    @DisplayName("교차 도메인: APP_MAPPING W 권한은 자체 W + 모든 교차 R을 포함한다")
    void convert_crossDomain_appMappingWrite_includesAllDependencies() {
        List<MenuPermission> permissions = List.of(new MenuPermission("APP_MAPPING", "W"));

        Set<GrantedAuthority> result = converter.convert(permissions);

        assertThat(authorityStrings(result))
                .containsExactlyInAnyOrder("APP_MAPPING:R", "APP_MAPPING:W", "ORG:R", "GATEWAY_MANAGEMENT:R", "TRX:R");
    }

    @Test
    @DisplayName("교차 도메인: 중복 권한은 하나로 합쳐진다 (APP_MAPPING + ROLE 모두 ORG:R 포함 시)")
    void convert_crossDomain_duplicateAuthoritiesAreMerged() {
        List<MenuPermission> permissions =
                List.of(new MenuPermission("APP_MAPPING", "R"), new MenuPermission("ROLE", "R"));

        Set<GrantedAuthority> result = converter.convert(permissions);

        // APP_MAPPING:R, ORG:R, GATEWAY_MANAGEMENT:R, TRX:R + ROLE:R, MENU:R
        assertThat(authorityStrings(result))
                .containsExactlyInAnyOrder(
                        "APP_MAPPING:R", "ORG:R", "GATEWAY_MANAGEMENT:R", "TRX:R", "ROLE:R", "MENU:R");
    }

    @Test
    @DisplayName("교차 도메인: ORG_CODE R 권한은 ORG:R을 포함한다")
    void convert_crossDomain_orgCodeRead_includesOrgRead() {
        List<MenuPermission> permissions = List.of(new MenuPermission("ORG_CODE", "R"));

        Set<GrantedAuthority> result = converter.convert(permissions);

        assertThat(authorityStrings(result)).containsExactlyInAnyOrder("ORG_CODE:R", "ORG:R", "CODE_GROUP:R");
    }

    @Test
    @DisplayName("매핑에 없는 메뉴의 권한은 빈 결과를 반환한다")
    void convert_unmappedMenu_returnsEmpty() {
        List<MenuPermission> permissions = List.of(new MenuPermission("UNKNOWN", "R"));

        Set<GrantedAuthority> result = converter.convert(permissions);

        assertThat(result).isEmpty();
    }

    private Set<String> authorityStrings(Set<GrantedAuthority> authorities) {
        return authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }
}
