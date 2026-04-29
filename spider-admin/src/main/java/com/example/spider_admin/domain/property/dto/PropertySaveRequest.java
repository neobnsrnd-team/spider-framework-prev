package com.example.spider_admin.domain.property.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 프로퍼티 저장 요청 DTO
 * C: Create, U: Update, D: Delete
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PropertySaveRequest {

    /**
     * 프로퍼티 그룹 ID
     */
    @NotBlank(message = "프로퍼티 그룹 ID는 필수입니다")
    @Size(max = 50, message = "프로퍼티 그룹 ID는 50자 이내여야 합니다")
    private String propertyGroupId;

    /**
     * 프로퍼티 ID
     */
    @NotBlank(message = "프로퍼티 ID는 필수입니다")
    @Size(max = 100, message = "프로퍼티 ID는 100자 이내여야 합니다")
    private String propertyId;

    /**
     * 프로퍼티명 (NOT NULL, VARCHAR2(100))
     */
    @Size(max = 100, message = "프로퍼티명은 100자 이내여야 합니다")
    private String propertyName;

    /**
     * 프로퍼티 설명 (NOT NULL, VARCHAR2(300))
     */
    @Size(max = 300, message = "프로퍼티 설명은 300자 이내여야 합니다")
    private String propertyDesc;

    /**
     * 데이터 타입 (C: String, N: Number, B: Boolean) - NULL 허용
     */
    @Pattern(regexp = "(^$|^[CNB]$)", message = "데이터 타입은 C, N, B 중 하나여야 합니다")
    private String dataType;

    /**
     * 유효 데이터 (NULL 허용, VARCHAR2(1000))
     */
    @Size(max = 1000, message = "유효 데이터는 1000자 이내여야 합니다")
    private String validData;

    /**
     * 기본값 (NULL 허용, VARCHAR2(1000))
     */
    @Size(max = 1000, message = "기본값은 1000자 이내여야 합니다")
    private String defaultValue;

    /**
     * CRUD 상태 (C: Create, U: Update, D: Delete)
     */
    @NotBlank(message = "CRUD 상태는 필수입니다")
    @Pattern(regexp = "^[CUD]$", message = "CRUD 상태는 C, U, D 중 하나여야 합니다")
    private String crud;
}
