package com.example.spider_admin.domain.property.dto;

import lombok.*;

/**
 * 프로퍼티 응답 DTO
 * 프로퍼티 상세 조회 시 사용
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyResponse {

    /**
     * 프로퍼티 그룹 ID
     */
    private String propertyGroupId;

    /**
     * 프로퍼티 ID
     */
    private String propertyId;

    /**
     * 프로퍼티명
     */
    private String propertyName;

    /**
     * 프로퍼티 설명
     */
    private String propertyDesc;

    /**
     * 데이터 타입 (C: String, N: Number, B: Boolean)
     */
    private String dataType;

    /**
     * 데이터 타입명 (String, Number, Boolean)
     */
    private String dataTypeName;

    /**
     * 유효 데이터
     */
    private String validData;

    /**
     * 기본값
     */
    private String defaultValue;

    /**
     * 최종 수정 사용자 ID
     */
    private String lastUpdateUserId;

    /**
     * 최종 수정 일시
     */
    private String lastUpdateDtime;

    /**
     * CRUD 상태 (C: Create, U: Update, D: Delete)
     */
    private String crud;
}
