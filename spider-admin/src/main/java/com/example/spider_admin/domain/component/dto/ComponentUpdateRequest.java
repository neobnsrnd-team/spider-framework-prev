package com.example.spider_admin.domain.component.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 컴포넌트 수정 요청 DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentUpdateRequest {

    @NotBlank(message = "컴포넌트명은 필수입니다")
    @Size(max = 100, message = "컴포넌트명은 100자 이하여야 합니다")
    private String componentName;

    @Size(max = 300, message = "컴포넌트 설명은 300자 이하여야 합니다")
    private String componentDesc;

    @NotBlank(message = "컴포넌트 유형은 필수입니다")
    @Size(max = 1, message = "컴포넌트 유형은 1자 이하여야 합니다")
    private String componentType;

    @NotBlank(message = "클래스명은 필수입니다")
    @Size(max = 100, message = "클래스명은 100자 이하여야 합니다")
    private String componentClassName;

    @NotBlank(message = "메서드명은 필수입니다")
    @Size(max = 50, message = "메서드명은 50자 이하여야 합니다")
    private String componentMethodName;

    @Size(max = 1, message = "생성 유형은 1자 이하여야 합니다")
    private String componentCreateType;

    @Size(max = 20, message = "Biz Group ID는 20자 이하여야 합니다")
    private String bizGroupId;

    @NotBlank(message = "사용여부는 필수입니다")
    private String useYn;

    @Valid
    private List<ComponentParamRequest> params;
}
