package com.example.spideradmin.domain.wasproperty.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * WAS 프로퍼티 수정 요청 DTO
 * PROPERTY_VALUE만 수정 가능
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class WasPropertyUpdateRequest {

    /**
     * 인스턴스 ID (필수)
     */
    @NotBlank(message = "인스턴스 ID는 필수입니다")
    private String instanceId;

    /**
     * 프로퍼티 그룹 ID (필수)
     */
    @NotBlank(message = "프로퍼티 그룹 ID는 필수입니다")
    private String propertyGroupId;

    /**
     * 프로퍼티 ID (필수)
     */
    @NotBlank(message = "프로퍼티 ID는 필수입니다")
    private String propertyId;

    /**
     * 설정값 (수정 가능)
     */
    @NotBlank(message = "프로퍼티 값은 필수입니다")
    private String propertyValue;

    /**
     * 프로퍼티 설명 (선택)
     */
    private String propertyDesc;

    /**
     * 변경 사유 (이력 관리용)
     */
    private String reason;

    /**
     * 현재 버전 (낙관적 락)
     */
    private Integer curVersion;
}
