package com.example.spideradmin.domain.validator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Validator 등록 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidatorCreateRequest {

    @NotBlank(message = "Validator ID는 필수입니다")
    @Size(max = 50, message = "Validator ID는 50자 이하여야 합니다")
    private String validatorId;

    @NotBlank(message = "Validator 명은 필수입니다")
    @Size(max = 100, message = "Validator 명은 100자 이하여야 합니다")
    private String validatorName;

    @NotBlank(message = "Site 구분은 필수입니다")
    private String bizDomain;

    @Size(max = 200, message = "Java 클래스명은 200자 이하여야 합니다")
    private String javaClassName;

    @Size(max = 500, message = "Validator 설명은 500자 이하여야 합니다")
    private String validatorDesc;

    @NotNull(message = "사용여부는 필수입니다")
    private String useYn;
}
