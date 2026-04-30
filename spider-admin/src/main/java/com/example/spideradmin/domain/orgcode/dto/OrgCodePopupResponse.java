package com.example.spideradmin.domain.orgcode.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgCodePopupResponse {
    private String code;
    private String codeName;
    private String orgCode; // null = 미매핑
    private String priority;
    private boolean existingMapping;
}
