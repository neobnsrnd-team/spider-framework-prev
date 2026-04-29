package com.example.spider_admin.domain.messagefield.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MessageField 응답 DTO
 * Message의 필드 정보를 담는 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageFieldResponse {

    private String orgId;
    private String messageId;
    private String messageFieldId;
    private Integer sortOrder;
    private String dataType;
    private Long dataLength;
    private Integer scale;
    private String align;
    private String filler;
    private String fieldType;
    private String useMode;
    private String requiredYn;
    private String fieldTag;
    private String codeGroup;
    private String defaultValue;
    private String testValue;
    private String remark;
    private String logYn;
    private String codeMappingYn;
    private String messageFieldName;
    private String messageFieldDesc;
    private String lastUpdateDtime;
    private String validationRuleId;
    private String lastUpdateUserId;
    private String fieldFormat;
    private String fieldFormatDesc;
    private String fieldOption;
    private Long fieldRepeatCnt;
}
