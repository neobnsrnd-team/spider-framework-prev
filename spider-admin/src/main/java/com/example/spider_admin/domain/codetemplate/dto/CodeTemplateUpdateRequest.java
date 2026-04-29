package com.example.spider_admin.domain.codetemplate.dto;

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
public class CodeTemplateUpdateRequest {

    @NotBlank(message = "템플릿명은 필수입니다")
    @Size(max = 100, message = "템플릿명은 100자 이내여야 합니다")
    private String templateName;

    @NotBlank(message = "템플릿 타입은 필수입니다")
    @Size(max = 20, message = "템플릿 타입은 20자 이내여야 합니다")
    private String templateType;

    @NotBlank(message = "템플릿 내용은 필수입니다")
    private String templateBody;

    @Size(max = 500, message = "설명은 500자 이내여야 합니다")
    private String description;

    private String useYn;

    private Integer sortOrder;
}
