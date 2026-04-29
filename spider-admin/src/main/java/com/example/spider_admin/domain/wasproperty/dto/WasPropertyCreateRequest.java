package com.example.spider_admin.domain.wasproperty.dto;

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
public class WasPropertyCreateRequest {

    @NotBlank(message = "인스턴스 ID는 필수입니다.")
    @Size(max = 4, message = "인스턴스 ID는 4자 이내여야 합니다.")
    private String instanceId;

    @NotBlank(message = "프로퍼티 그룹 ID는 필수입니다.")
    @Size(max = 20, message = "프로퍼티 그룹 ID는 20자 이내여야 합니다.")
    private String propertyGroupId;

    @NotBlank(message = "프로퍼티 ID는 필수입니다.")
    @Size(max = 50, message = "프로퍼티 ID는 50자 이내여야 합니다.")
    private String propertyId;

    @Size(max = 1000, message = "프로퍼티 값은 1000자 이내여야 합니다.")
    private String propertyValue;

    @Size(max = 1000, message = "프로퍼티 설명은 1000자 이내여야 합니다.")
    private String propertyDesc;
}
