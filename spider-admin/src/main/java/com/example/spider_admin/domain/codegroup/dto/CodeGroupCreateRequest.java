package com.example.spider_admin.domain.codegroup.dto;

import com.example.spider_admin.domain.code.dto.CodeCreateRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeGroupCreateRequest {

    @Size(max = 8, message = "코드 그룹 ID는 8자 이내여야 합니다")
    private String codeGroupId;

    @NotBlank(message = "코드 그룹명은 필수입니다")
    @Size(max = 100, message = "코드 그룹명은 100자 이내여야 합니다")
    private String codeGroupName;

    @Size(max = 200, message = "코드 그룹 설명은 200자 이내여야 합니다")
    private String codeGroupDesc;

    @Size(max = 20, message = "업무 그룹 ID는 20자 이내여야 합니다")
    private String bizGroupId;

    private List<CodeCreateRequest> codes;
}
