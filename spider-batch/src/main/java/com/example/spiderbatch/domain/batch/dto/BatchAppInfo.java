package com.example.spiderbatch.domain.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @file BatchAppInfo.java
 * @description FWK_BATCH_APP에서 조회한 배치 APP 기본 정보 (이름 + CRON 표현식).
 *              실행 중인 배치 조회 시 배치 이름과 스케줄 정보를 담아 반환한다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchAppInfo {

    /** FWK_BATCH_APP.BATCH_APP_NAME — 배치 앱 이름 */
    private String batchAppName;

    /** FWK_BATCH_APP.CRON_TEXT — CRON 표현식 (스케줄 미등록 시 null) */
    private String cronText;

    /** FWK_BATCH_APP.SLA_SECONDS — 최대 허용 실행 시간(초), NULL이면 SLA 미적용 */
    private Integer slaSeconds;
}
