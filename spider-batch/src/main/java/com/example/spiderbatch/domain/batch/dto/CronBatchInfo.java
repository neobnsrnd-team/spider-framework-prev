package com.example.spiderbatch.domain.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @file CronBatchInfo.java
 * @description WAS 기동 시 Quartz 자동 등록 대상 조회 결과 DTO.
 *              FWK_WAS_EXEC_BATCH JOIN FWK_BATCH_APP 에서 CRON_TEXT가 설정된 배치 정보를 담는다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CronBatchInfo {

    /** FWK_BATCH_APP.BATCH_APP_ID */
    private String batchAppId;

    /** FWK_BATCH_APP.BATCH_APP_FILE_NAME — JobRegistry에 등록된 Job Bean 이름 */
    private String batchAppFileName;

    /** FWK_BATCH_APP.CRON_TEXT — Quartz CronTrigger에 사용할 Cron 표현식 */
    private String cronText;
}
