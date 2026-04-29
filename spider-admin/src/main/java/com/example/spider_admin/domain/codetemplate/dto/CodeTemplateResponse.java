package com.example.spider_admin.domain.codetemplate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeTemplateResponse {

    private String templateId;
    private String templateName;
    private String templateType;
    private String templateBody;
    private String description;
    private String useYn;
    private Integer sortOrder;
    private String lastUpdateDtime;
    private String lastUpdateUserId;
}
