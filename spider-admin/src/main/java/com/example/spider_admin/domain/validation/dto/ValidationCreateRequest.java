package com.example.spider_admin.domain.validation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Validation 등록 요청 DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationCreateRequest {

    @NotBlank(message = "Validation ID는 필수입니다")
    @Size(max = 50, message = "Validation ID는 50자 이하여야 합니다")
    private String validationId;

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
