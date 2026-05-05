package com.example.spideradmin.domain.orgcode.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrgCodeRowRequest {
    private String crudType; // "C", "U", "D"
    private String code; // 표준 CODE
    private String orgCode; // 기관 코드 (신규/변경값)
    private String oldOrgCode; // U/D 시 기존 orgCode (PK 포함)
    private String priority;
}
