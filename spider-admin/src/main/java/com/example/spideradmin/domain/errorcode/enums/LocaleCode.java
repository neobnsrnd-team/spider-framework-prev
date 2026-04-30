package com.example.spideradmin.domain.errorcode.enums;

import com.example.spideradmin.global.common.base.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 로케일 코드 Enum
 * 다국어 지원을 위한 언어 코드 열거형
 */
@Getter
@RequiredArgsConstructor
public enum LocaleCode implements BaseEnum {
    EN("EN", "English"),
    KO("KO", "한국어");

    private final String code;
    private final String description;

    /**
     * DB 코드 값으로부터 LocaleCode Enum을 찾습니다.
     * @param code DB에 저장된 코드 값 (EN, KO)
     * @return 해당하는 LocaleCode, null이거나 매칭되지 않으면 null 반환
     */
    public static LocaleCode fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (LocaleCode locale : LocaleCode.values()) {
            if (locale.code.equalsIgnoreCase(code)) {
                return locale;
            }
        }
        return null;
    }

    /**
     * 코드로 설명 조회
     * @param code DB에 저장된 코드 값
     * @return 코드에 해당하는 설명, 없으면 코드 그대로 반환
     */
    public static String getDescriptionByCode(String code) {
        LocaleCode locale = fromCode(code);
        return locale != null ? locale.description : code;
    }
}
