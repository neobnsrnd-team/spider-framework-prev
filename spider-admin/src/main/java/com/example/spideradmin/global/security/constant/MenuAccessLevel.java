package com.example.spideradmin.global.security.constant;

/**
 * 메뉴 접근 권한 레벨
 *
 * FWK_USER_MENU 테이블의 AUTH_CODE 값과 매핑됩니다.
 * - READ: 읽기 권한 (R)
 * - WRITE: 쓰기 권한 (W) - 읽기 권한 포함
 */
public enum MenuAccessLevel {
    READ("R", "읽기"),
    WRITE("W", "쓰기");

    private final String code;
    private final String description;

    MenuAccessLevel(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 권한 코드로 MenuAccessLevel 조회
     *
     * @param code 권한 코드 (R, W)
     * @return MenuAccessLevel 또는 null
     */
    public static MenuAccessLevel fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (MenuAccessLevel level : values()) {
            if (level.code.equals(code)) {
                return level;
            }
        }
        return null;
    }

    /**
     * 사용자 권한이 요청 권한을 충족하는지 확인
     * - WRITE 권한은 READ 권한을 포함
     *
     * @param userLevel 사용자 보유 권한
     * @param requiredLevel 요청 권한
     * @return 권한 충족 여부
     */
    public static boolean hasAccess(MenuAccessLevel userLevel, MenuAccessLevel requiredLevel) {
        if (userLevel == null || requiredLevel == null) {
            return false;
        }
        // WRITE 권한은 READ 권한 포함
        if (userLevel == WRITE) {
            return true;
        }
        // READ 권한은 READ만 허용
        return userLevel == READ && requiredLevel == READ;
    }
}
