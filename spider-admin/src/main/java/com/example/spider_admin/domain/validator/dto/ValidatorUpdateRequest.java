package com.example.spider_admin.domain.validator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Validator 수정 요청 DTO
 * validatorId는 PathVariable로 전달
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidatorUpdateRequest {

    @NotBlank(message = "Validator 명은 필수입니다")
    @Size(max = 100, message = "Validator 명은 100자 이하여야 합니다")
    private String validatorName;

    @NotBlank(message = "Site 구분은 필수입니다")
    private String bizDomain;

    @NotBlank(message = "Application 명은 필수입니다")
    @Size(max = 200, message = "Application 명은 200자 이하여야 합니다")
    private String javaClassName;

    @Size(max = 500, message = "Validator 설명은 500자 이하여야 합니다")
    private String validatorDesc;

    @NotNull(message = "사용여부는 필수입니다")
    private String useYn;
}
