package com.example.admin_demo.domain.emergencynotice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 긴급공지 노출 설정 변경 요청 DTO
 *
 * <p>공지 모달의 닫기 버튼 및 오늘 하루 보지 않기 체크박스 노출 여부를 담는다.
 * 변경 즉시 배포 중이면 biz-channel에 재동기화된다.
 *
 * @param closeableYn  닫기 버튼 노출 여부 (Y: 표시 / N: 강제 노출)
 * @param hideTodayYn 오늘 하루 보지 않기 체크박스 노출 여부 (Y: 표시 / N: 숨김)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyNoticeSettingsRequest {

    /** 닫기 버튼 노출 여부 — Y: 표시(기본) / N: 강제 노출(critical 장애 시) */
    @NotNull(message = "closeableYn은 필수입니다.")
    @Pattern(regexp = "^[YN]$", message = "closeableYn은 Y 또는 N만 허용됩니다.")
    private String closeableYn;

    /** 오늘 하루 보지 않기 체크박스 노출 여부 — Y: 표시(기본) / N: 숨김 */
    @NotNull(message = "hideTodayYn은 필수입니다.")
    @Pattern(regexp = "^[YN]$", message = "hideTodayYn은 Y 또는 N만 허용됩니다.")
    private String hideTodayYn;
}
