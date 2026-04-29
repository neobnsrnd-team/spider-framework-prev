package com.example.spider_admin.domain.validator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Validator 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidatorResponse {

    private String validatorId;
    private String validatorName;
    private String validatorDesc;
    private String bizDomain;
    private String bizDomainName;
    private String javaClassName;
    private String useYn;
    private String lastUpdateDtime;
    private String lastUpdateUserId;
}
