package com.example.spideradmin.domain.emergencynotice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 긴급공지 배포 상태 응답 DTO
 *
 * FWK_PROPERTY 'notice'.USE_YN 행의 배포 관련 컬럼과
 * CLOSEABLE_YN·HIDE_TODAY_YN 행의 노출 설정을 함께 담는다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyNoticeDeployStatusResponse {

    /**
     * 배포 상태
     * DRAFT: 미배포 / DEPLOYED: 배포 중 / ENDED: 배포 종료
     */
    private String deployStatus;

    /** 배포 시작 일시 (yyyyMMddHHmmss, nullable) */
    private String startDtime;

    /** 배포 종료 일시 (yyyyMMddHHmmss, nullable) */
    private String endDtime;

    /**
     * 닫기 버튼 노출 여부
     * Y: 닫기 버튼 표시 (기본) / N: 강제 노출 (critical 장애 시 사용자 접근 차단)
     */
    private String closeableYn;

    /**
     * 오늘 하루 보지 않기 체크박스 노출 여부
     * Y: 표시 (기본) / N: 숨김
     */
    private String hideTodayYn;
}
