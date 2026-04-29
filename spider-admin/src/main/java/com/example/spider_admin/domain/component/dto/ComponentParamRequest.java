package com.example.spider_admin.domain.component.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 컴포넌트 파라미터 항목 DTO (등록/수정 요청 내 params 리스트 원소) */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentParamRequest {

    private Integer paramSeqNo;

    @NotBlank(message = "파라미터 키는 필수입니다")
    @Size(max = 100, message = "파라미터 키는 100자 이하여야 합니다")
    private String paramKey;

    @Size(max = 300, message = "파라미터 설명은 300자 이하여야 합니다")
    private String paramDesc;

    @Size(max = 500, message = "기본 파라미터 값은 500자 이하여야 합니다")
    private String defaultParamValue;
}
