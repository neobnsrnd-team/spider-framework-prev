package com.example.spideradmin.domain.emergencynotice.dto;

import com.example.spideradmin.domain.emergencynotice.DisplayType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 긴급공지 일괄 저장 요청 DTO
 *
 * 언어별 공지(KO/EN)와 노출 타입을 한 번에 저장한다.
 * 노출 타입(displayType)은 USE_YN 행의 DEFAULT_VALUE에 저장된다.
 *   A: 전체 / B: 기업 / C: 개인 / N: 사용안함
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class EmergencyNoticeBulkSaveRequest {

    /**
     * 언어별 긴급공지 목록 (EMERGENCY_KO, EMERGENCY_EN)
     */
    @NotNull(message = "공지 목록은 필수입니다")
    @Valid
    private List<EmergencyNoticeSaveRequest> notices;

    /**
     * 긴급공지 노출 타입 (A: 전체 / B: 기업 / C: 개인 / N: 사용안함)
     * FWK_PROPERTY 'notice'.USE_YN 행의 DEFAULT_VALUE에 저장된다.
     * 유효하지 않은 값은 Jackson 역직렬화 단계에서 400으로 거부된다.
     */
    @NotNull(message = "노출 타입은 필수입니다")
    private DisplayType displayType;
}
