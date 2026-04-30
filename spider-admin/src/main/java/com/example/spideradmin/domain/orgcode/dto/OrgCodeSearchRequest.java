package com.example.spideradmin.domain.orgcode.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrgCodeSearchRequest {
    private String orgId;
    private String codeGroupId;
    private Integer page = 1;
    private Integer size = 20;
}
