package com.example.spideradmin.domain.wasproperty.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * WAS 프로퍼티 배치 저장 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WasPropertyBatchSaveRequest {

    /**
     * 인스턴스 ID
     */
    @NotBlank(message = "인스턴스 ID는 필수입니다")
    private String instanceId;

    /**
     * 프로퍼티 그룹 ID
     */
    @NotBlank(message = "프로퍼티 그룹 ID는 필수입니다")
    private String propertyGroupId;

    /**
     * 프로퍼티 ID
     */
    @NotBlank(message = "프로퍼티 ID는 필수입니다")
    private String propertyId;

    /**
     * 설정된 값
     */
    private String propertyValue;

    /**
     * 특이사항 (프로퍼티 설명)
     */
    private String propertyDesc;

    /**
     * CRUD 액션 (C: 생성, U: 수정, D: 삭제)
     */
    @NotBlank(message = "CRUD 액션은 필수입니다")
    private String crud;
}
