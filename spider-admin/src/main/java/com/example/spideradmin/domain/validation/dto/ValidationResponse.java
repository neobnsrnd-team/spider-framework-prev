package com.example.spideradmin.domain.validation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Validation 응답 DTO */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationResponse {

    private String validationId;
    private String validationDesc;
    private String fieldEventText;
    private String maskText;
    private String charTypeText;
    private String maxValueText;
    private String minValueText;
    private String submitEventText;
    private String javaClassName;
    private String lastUpdateDtime;
    private String lastUpdateUserId;
}
