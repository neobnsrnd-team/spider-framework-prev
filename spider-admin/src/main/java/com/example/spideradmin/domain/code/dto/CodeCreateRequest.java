package com.example.spideradmin.domain.code.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeCreateRequest {

    @NotBlank(message = "코드 그룹 ID는 필수입니다")
    @Size(max = 8, message = "코드 그룹 ID는 8자 이내여야 합니다")
    private String codeGroupId;

    @NotBlank(message = "코드는 필수입니다")
    @Size(max = 50, message = "코드는 50자 이내여야 합니다")
    private String code;

    @NotBlank(message = "코드명은 필수입니다")
    @Size(max = 100, message = "코드명은 100자 이내여야 합니다")
    private String codeName;

    @Size(max = 200, message = "코드 설명은 200자 이내여야 합니다")
    private String codeDesc;

    private Integer sortOrder;

    @Size(max = 1, message = "사용 여부는 1자 이내여야 합니다")
    private String useYn;

    @Size(max = 300, message = "코드 영문명은 300자 이내여야 합니다")
    private String codeEngname;
}
