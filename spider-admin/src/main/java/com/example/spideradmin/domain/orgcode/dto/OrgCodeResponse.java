package com.example.spideradmin.domain.orgcode.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgCodeResponse {
    private String orgId;
    private String orgName;
    private String codeGroupId;
    private String codeGroupName;
    private String code;
    private String codeName;
    private String orgCode;
    private String priority;
}
