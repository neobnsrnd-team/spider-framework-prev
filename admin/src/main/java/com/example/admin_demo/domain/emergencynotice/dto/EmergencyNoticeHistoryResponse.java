package com.example.admin_demo.domain.emergencynotice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 긴급공지 배포 이력 응답 DTO
 *
 * FWK_PROPERTY_HISTORY 테이블의 'notice'.USE_YN 행 이력을 담는다.
 * USE_YN 행만 조회하며, 같은 VERSION의 EMERGENCY_KO·CLOSEABLE_YN·HIDE_TODAY_YN 행을
 * LEFT JOIN하여 변경 상세 정보를 함께 반환한다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyNoticeHistoryResponse {

    /** 이력 버전 번호 */
    private Integer version;

    /**
     * 변경 구분 (REASON 컬럼)
     * '배포' / '배포 종료' / '내용 수정' / '설정 변경'
     */
    private String reason;

    /** 변경 일시 (yyyyMMddHHmmss) */
    private String lastUpdateDtime;

    /** 변경자 ID */
    private String lastUpdateUserId;

    /**
     * 한국어 공지 제목 (같은 VERSION의 EMERGENCY_KO 행 PROPERTY_DESC).
     * 배포·배포종료·내용수정 구분의 상세 표시에 사용된다.
     */
    private String titleKo;

    /**
     * 한국어 공지 내용 (같은 VERSION의 EMERGENCY_KO 행 DEFAULT_VALUE).
     * _$BR 마커 포함 가능. 배포·배포종료·내용수정 구분의 상세 표시에 사용된다.
     */
    private String contentKo;

    /**
     * 영어 공지 제목 (같은 VERSION의 EMERGENCY_EN 행 PROPERTY_DESC).
     * 배포·배포종료·내용수정 구분의 상세 표시에 사용된다.
     */
    private String titleEn;

    /**
     * 영어 공지 내용 (같은 VERSION의 EMERGENCY_EN 행 DEFAULT_VALUE).
     * _$BR 마커 포함 가능. 배포·배포종료·내용수정 구분의 상세 표시에 사용된다.
     */
    private String contentEn;

    /**
     * 닫기 버튼 노출 여부 (같은 VERSION의 CLOSEABLE_YN 행 DEFAULT_VALUE, Y/N).
     * 설정 변경 구분의 상세 표시에 사용된다.
     */
    private String closeableYn;

    /**
     * 오늘 하루 보지 않기 노출 여부 (같은 VERSION의 HIDE_TODAY_YN 행 DEFAULT_VALUE, Y/N).
     * 설정 변경 구분의 상세 표시에 사용된다.
     */
    private String hideTodayYn;
}
