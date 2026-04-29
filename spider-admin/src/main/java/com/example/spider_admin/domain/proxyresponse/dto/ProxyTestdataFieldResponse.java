package com.example.spider_admin.domain.proxyresponse.dto;

import lombok.*;

/**
 * 당발 대응답 테스트용 전문 필드 응답 DTO
 * 기관 전문 필드 + 표준 전문 필드 JOIN 결과 + makeRealValue 계산 결과 포함
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ProxyTestdataFieldResponse {

    private String messageId;
    private String messageFieldId;
    private String messageFieldName;
    private Long dataLength;
    private String dataType;
    private String align;
    private String requiredYn;
    private String fieldType;
    private Integer sortOrder;
    private Integer scale;

    /** 기관 전문 USE_MODE */
    private String orgUseMode;

    /** 기관 전문 DEFAULT_VALUE */
    private String orgDefaultValue;

    /** 표준 전문 USE_MODE */
    private String stdUseMode;

    /** 표준 전문 DEFAULT_VALUE */
    private String stdDefaultValue;

    /** 기관 전문 TEST_VALUE */
    private String testValue;

    // === makeRealValue 계산 결과 ===

    /** 화면에 표시할 실제 값 */
    private String realValue;

    /** 읽기 전용 여부 */
    private boolean readOnly;

    /** 숨김 여부 (시스템 키워드 등) */
    private boolean hidden;

    /** 값 유형 코멘트 (예: "시스템 키워드", "기관 전문 초기값" 등) */
    private String valueComment;
}
