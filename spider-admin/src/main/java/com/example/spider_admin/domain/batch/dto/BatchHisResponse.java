package com.example.spider_admin.domain.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 배치 실행 이력 Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchHisResponse {

    /**
     * 배치 애플리케이션 ID
     */
    private String batchAppId;

    /**
     * 인스턴스 ID
     */
    private String instanceId;

    /**
     * 배치 실행 일자 (YYYYMMDD)
     */
    private String batchDate;

    /**
     * 배치 실행 순번
     */
    private Integer batchExecuteSeq;

    /**
     * 로그 일시 (YYYYMMDDHH24MISSFF3)
     */
    private String logDtime;

    /**
     * 배치 종료 일시
     */
    private String batchEndDtime;

    /**
     * 결과 리턴 코드 (0: 시작됨, 1: 정상 종료, -1: 대상건수 알수없음)
     */
    private String resRtCode;

    /**
     * 최종 수정 사용자 ID
     */
    private String lastUpdateUserId;

    /**
     * 오류 코드
     */
    private String errorCode;

    /**
     * 오류 사유
     */
    private String errorReason;

    /**
     * 대상 건수
     */
    private Integer recordCount;

    /**
     * 실행 건수
     */
    private Integer executeCount;

    /**
     * 성공 건수
     */
    private Integer successCount;

    /**
     * 실패 건수
     */
    private Integer failCount;

    // 조인 필드 (조회용)
    /**
     * 배치 애플리케이션명
     */
    private String batchAppName;

    /**
     * 인스턴스명
     */
    private String instanceName;

    /**
     * 배치 APP 파일명 (조인용)
     */
    private String batchAppFileName;

    /**
     * CRON 표현식 (조인용)
     */
    private String cronText;
}
