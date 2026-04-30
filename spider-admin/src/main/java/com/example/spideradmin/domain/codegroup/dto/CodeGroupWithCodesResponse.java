package com.example.spideradmin.domain.codegroup.dto;

import com.example.spideradmin.domain.code.dto.CodeResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeGroupWithCodesResponse {

    private String codeGroupId;
    private String codeGroupName;
    private String codeGroupDesc;
    private String lastUpdateDtime;
    private String lastUpdateUserId;
    private String bizGroupId;
    private Integer codeCount;
    private List<CodeResponse> codes;
}
