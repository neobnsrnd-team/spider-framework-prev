package com.example.spideradmin.global.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 메뉴 설정 프로퍼티
 *
 * <p>DB를 변경하지 않고 이 프로젝트에서만 메뉴 표시 여부를 제어한다.
 *
 * <p>설정 예시 ({@code application.yml}):
 *
 * <pre>{@code
 * menu:
 *   hidden-menu-ids:
 *     - acl_manage
 *     - v3_batch_manage
 * }</pre>
 */
@Component
@ConfigurationProperties(prefix = "menu")
@Getter
@Setter
public class MenuProperties {

    /**
     * 사이드바에서 숨길 메뉴 ID 목록.
     *
     * <p>지정된 메뉴와 그 하위 메뉴 전체가 사이드바에서 제외된다.
     * DB의 {@code DISPLAY_YN} / {@code USE_YN} 값은 변경하지 않는다.
     */
    private List<String> hiddenMenuIds = new ArrayList<>();
}
