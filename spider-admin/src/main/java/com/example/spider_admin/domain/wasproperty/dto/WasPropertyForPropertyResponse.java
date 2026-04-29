package com.example.spider_admin.domain.wasproperty.dto;

import lombok.*;

/**
 * 프로퍼티 기준 WAS 프로퍼티 응답 DTO
 * 특정 프로퍼티에 대한 WAS 인스턴스별 설정 값 조회용
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WasPropertyForPropertyResponse {

    private String instanceId;
    private String instanceName;
    private String propertyGroupId;
    private String propertyId;
    private String propertyValue;
    private String propertyDesc;
    private String hasExistingData;
}
