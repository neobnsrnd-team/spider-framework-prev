package com.example.spideradmin.domain.code.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeWithGroupResponse {

    private String codeGroupId;
    private String code;
    private String codeName;
    private String codeDesc;
    private Integer sortOrder;
    private String useYn;
    private String codeEngname;
    private String lastUpdateDtime;
    private String lastUpdateUserId;
    private String codeGroupName;
}
