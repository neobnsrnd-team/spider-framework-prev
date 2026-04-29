package com.example.spider_admin.domain.property.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.*;

/**
 * 프로퍼티 그룹 생성 요청 DTO
 * FWK_CODE(FR00006) INSERT + FWK_PROPERTY batch INSERT
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyGroupCreateRequest {

    @NotBlank(message = "프로퍼티 그룹 ID는 필수입니다.")
    @Size(max = 100, message = "프로퍼티 그룹 ID는 100자 이하여야 합니다.")
    private String propertyGroupId;

    @NotBlank(message = "프로퍼티 그룹명은 필수입니다.")
    @Size(max = 200, message = "프로퍼티 그룹명은 200자 이하여야 합니다.")
    private String propertyGroupName;

    @NotEmpty(message = "하위 프로퍼티는 최소 1개 이상이어야 합니다.")
    @Valid
    private List<PropertyCreateRequest> properties;
}
