package com.example.spider_admin.global.security.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.spider_admin.global.security.constant.MenuAccessLevel;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MenuResourcePermissions 테스트")
class MenuResourcePermissionsTest {

    private MenuResourcePermissions permissions;

    @BeforeEach
    void setUp() {
        permissions = new MenuResourcePermissions();
        permissions.setPermissions(Map.of(
                "USER", Map.of("R", "USER:R", "W", "USER:W"),
                "ROLE", Map.of("R", "ROLE:R, MENU:R", "W", "ROLE:W, MENU:R"),
                "APP_MAPPING",
                        Map.of(
                                "R",
                                "APP_MAPPING:R, ORG:R, GATEWAY_MANAGEMENT:R, TRX:R",
                                "W",
                                "APP_MAPPING:W, ORG:R, GATEWAY_MANAGEMENT:R, TRX:R")));
    }

    @Test
    @DisplayName("READ 레벨이면 R 매핑만 반환한다")
    void getDerivedResourceAuthorities_read_returnsReadOnly() {
        Set<String> result = permissions.getDerivedResourceAuthorities("USER", MenuAccessLevel.READ);

        assertThat(result).containsExactly("USER:R");
    }

    @Test
    @DisplayName("WRITE 레벨이면 R + W 매핑을 모두 반환한다")
    void getDerivedResourceAuthorities_write_returnsReadAndWrite() {
        Set<String> result = permissions.getDerivedResourceAuthorities("USER", MenuAccessLevel.WRITE);

        assertThat(result).containsExactlyInAnyOrder("USER:R", "USER:W");
    }

    @Test
    @DisplayName("콤마 구분 다중 리소스를 올바르게 파싱한다")
    void getDerivedResourceAuthorities_multiResource_parsesCorrectly() {
        Set<String> result = permissions.getDerivedResourceAuthorities("ROLE", MenuAccessLevel.READ);

        assertThat(result).containsExactlyInAnyOrder("ROLE:R", "MENU:R");
    }

    @Test
    @DisplayName("WRITE 레벨에서 R/W 다중 리소스를 모두 반환한다")
    void getDerivedResourceAuthorities_multiResource_write() {
        Set<String> result = permissions.getDerivedResourceAuthorities("ROLE", MenuAccessLevel.WRITE);

        assertThat(result).containsExactlyInAnyOrder("ROLE:R", "MENU:R", "ROLE:W");
    }

    @Test
    @DisplayName("존재하지 않는 메뉴 ID는 빈 Set을 반환한다")
    void getDerivedResourceAuthorities_unknownMenu_returnsEmpty() {
        Set<String> result = permissions.getDerivedResourceAuthorities("UNKNOWN", MenuAccessLevel.READ);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("교차 도메인 권한: APP_MAPPING READ는 ORG:R, GATEWAY_MANAGEMENT:R, TRX:R을 포함한다")
    void getDerivedResourceAuthorities_crossDomain_appMappingRead() {
        Set<String> result = permissions.getDerivedResourceAuthorities("APP_MAPPING", MenuAccessLevel.READ);

        assertThat(result).containsExactlyInAnyOrder("APP_MAPPING:R", "ORG:R", "GATEWAY_MANAGEMENT:R", "TRX:R");
    }

    @Test
    @DisplayName("교차 도메인 권한: APP_MAPPING WRITE는 자체 W + 교차 R을 모두 포함한다")
    void getDerivedResourceAuthorities_crossDomain_appMappingWrite() {
        Set<String> result = permissions.getDerivedResourceAuthorities("APP_MAPPING", MenuAccessLevel.WRITE);

        assertThat(result)
                .containsExactlyInAnyOrder("APP_MAPPING:R", "ORG:R", "GATEWAY_MANAGEMENT:R", "TRX:R", "APP_MAPPING:W");
    }

    @Test
    @DisplayName("빈 permissions에서도 에러 없이 빈 Set을 반환한다")
    void getDerivedResourceAuthorities_emptyPermissions_returnsEmpty() {
        MenuResourcePermissions empty = new MenuResourcePermissions();

        Set<String> result = empty.getDerivedResourceAuthorities("USER", MenuAccessLevel.READ);

        assertThat(result).isEmpty();
    }
}
