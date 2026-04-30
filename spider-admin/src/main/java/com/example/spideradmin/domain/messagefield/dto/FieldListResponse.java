package com.example.spideradmin.domain.messagefield.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 전문 필드 목록 조회 응답 DTO
 * GET /api/messages/{messageId}/fields 응답에 사용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldListResponse {

    private Integer sortOrder;
    private String messageFieldName;
    private String messageFieldId;
    private String dataType;
    private Long dataLength;
    private String align;
    private String requiredYn;
    private Integer scale;
    private String filler;
    private String useMode;
    private String codeGroup;
    private String codeMappingYn;
    private String defaultValue;
    private String remark;
    private String logYn;
    private String validationRuleId;
    private String fieldTag;
}
