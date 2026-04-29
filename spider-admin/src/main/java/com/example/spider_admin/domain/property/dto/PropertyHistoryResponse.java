package com.example.spider_admin.domain.property.dto;

import lombok.*;

/**
 * 프로퍼티 이력 데이터 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyHistoryResponse {
    private String propertyGroupId;
    private String propertyId;
    private Integer version;
    private String propertyName;
    private String propertyDesc;
    private String dataType;
    private String validData;
    private String defaultValue;
    private String reason;
    private String lastUpdateUserId;
    private String lastUpdateDtime;
}
