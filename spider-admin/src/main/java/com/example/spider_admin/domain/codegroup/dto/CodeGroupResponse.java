package com.example.spider_admin.domain.codegroup.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeGroupResponse {

    private String codeGroupId;
    private String codeGroupName;
    private String codeGroupDesc;
    private String lastUpdateDtime;
    private String lastUpdateUserId;
    private String bizGroupId;
    private Integer codeCount;
}
