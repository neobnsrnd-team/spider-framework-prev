package com.example.spideradmin.domain.validation.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Validation 수정 요청 DTO
 * validationId는 PathVariable로 전달
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationUpdateRequest {

    @Size(max = 500, message = "Validation 설명은 500자 이하여야 합니다")
    private String validationDesc;

    @Size(max = 4000, message = "Field Event Text는 4000자 이하여야 합니다")
    private String fieldEventText;

    @Size(max = 200, message = "Mask Text는 200자 이하여야 합니다")
    private String maskText;

    @Size(max = 200, message = "Char Type Text는 200자 이하여야 합니다")
    private String charTypeText;

    @Size(max = 200, message = "Max Value Text는 200자 이하여야 합니다")
    private String maxValueText;

    @Size(max = 200, message = "Min Value Text는 200자 이하여야 합니다")
    private String minValueText;

    @Size(max = 4000, message = "Submit Event Text는 4000자 이하여야 합니다")
    private String submitEventText;

    @Size(max = 500, message = "Java Class Name은 500자 이하여야 합니다")
    private String javaClassName;
}
