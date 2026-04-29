package com.example.spider_admin.domain.property.dto;

import lombok.*;

/**
 * 프로퍼티 그룹 응답 DTO
 * 프로퍼티 그룹 목록 조회 시 사용
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyGroupResponse {

    /**
     * 프로퍼티 그룹 ID
     */
    private String propertyGroupId;

    /**
     * 프로퍼티 그룹명 (그룹 내 첫 번째 프로퍼티명)
     */
    private String propertyGroupName;

    /**
     * 프로퍼티 개수
     */
    private Integer propertyCount;

    /**
     * 최종 수정 일시
     */
    private String lastUpdateDtime;

    /**
     * 최종 수정 사용자 ID
     */
    private String lastUpdateUserId;
}
