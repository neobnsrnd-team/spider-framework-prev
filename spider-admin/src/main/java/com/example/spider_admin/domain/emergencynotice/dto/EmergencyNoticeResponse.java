package com.example.spider_admin.domain.emergencynotice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 긴급공지 조회 응답 DTO
 *
 * FWK_PROPERTY 테이블의 'notice' 그룹에서 조회한 데이터를 담는다.
 * ASIS 매핑:
 *   PROPERTY_DESC  → title   (긴급공지 제목)
 *   DEFAULT_VALUE  → content (긴급공지 내용)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyNoticeResponse {

    /** 프로퍼티 ID (EMERGENCY_KO / EMERGENCY_EN) */
    private String propertyId;

    /** 긴급공지 제목 (← PROPERTY_DESC) */
    private String title;

    /** 긴급공지 내용 (← DEFAULT_VALUE) */
    private String content;

    /** 최종 수정 일시 */
    private String lastUpdateDtime;

    /** 최종 수정 사용자 ID */
    private String lastUpdateUserId;
}
