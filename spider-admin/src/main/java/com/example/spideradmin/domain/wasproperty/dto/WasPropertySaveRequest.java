package com.example.spideradmin.domain.wasproperty.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * WAS 프로퍼티 저장 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WasPropertySaveRequest {

    @NotBlank(message = "인스턴스 ID는 필수입니다")
    private String instanceId;

    @NotBlank(message = "프로퍼티 그룹 ID는 필수입니다")
    private String propertyGroupId;

    @NotBlank(message = "프로퍼티 ID는 필수입니다")
    private String propertyId;

    private String propertyValue;

    private String propertyDesc;
}
