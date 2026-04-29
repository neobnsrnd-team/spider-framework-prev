package com.example.spider_admin.domain.orgcode.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrgCodeSaveRequest {
    @NotBlank(message = "기관 ID는 필수입니다")
    private String orgId;

    @NotBlank(message = "코드그룹 ID는 필수입니다")
    private String codeGroupId;

    @NotNull(message = "행 목록은 필수입니다")
    @Valid
    private List<OrgCodeRowRequest> rows;
}
