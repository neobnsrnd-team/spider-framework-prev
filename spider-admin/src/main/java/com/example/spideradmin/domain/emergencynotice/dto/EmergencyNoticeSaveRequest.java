package com.example.spideradmin.domain.emergencynotice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 언어별 긴급공지 저장 요청 DTO
 *
 * PROPERTY_ID는 EMERGENCY_KO 또는 EMERGENCY_EN만 허용한다.
 * title   → PROPERTY_DESC (긴급공지 제목)
 * content → DEFAULT_VALUE (긴급공지 내용)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class EmergencyNoticeSaveRequest {

    /**
     * 프로퍼티 ID (EMERGENCY_KO 또는 EMERGENCY_EN)
     */
    @NotBlank(message = "propertyId는 필수입니다")
    @Pattern(regexp = "EMERGENCY_KO|EMERGENCY_EN", message = "propertyId는 EMERGENCY_KO 또는 EMERGENCY_EN이어야 합니다")
    private String propertyId;

    /**
     * 긴급공지 제목 (→ PROPERTY_DESC)
     */
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 300, message = "제목은 300자 이내여야 합니다")
    private String title;

    /**
     * 긴급공지 내용 (→ DEFAULT_VALUE)
     */
    @NotBlank(message = "내용은 필수입니다")
    @Size(max = 1000, message = "내용은 1000자 이내여야 합니다")
    private String content;
}
