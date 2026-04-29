package com.example.spider_admin.global.common.enums;

import com.example.spider_admin.global.common.base.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <h3>업무 도메인(SITE 구분) 코드</h3>
 * <p>전문의 업무 도메인을 정의합니다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum BizDomain implements BaseEnum {
    BANKING("뱅킹", "뱅킹전용"),
    CORPORATE("기업", "기업뱅킹"),
    COMMON("공통", "공통"),
    PORTAL("포탈", "포탈전용"),
    COMMUNITY("CM", "커뮤니티"),
    CARD("카드", "카드전용");

    private final String code;
    private final String description;

    public static BizDomain fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (BizDomain domain : BizDomain.values()) {
            if (domain.code.equals(code)) {
                return domain;
            }
        }
        return null;
    }
}
