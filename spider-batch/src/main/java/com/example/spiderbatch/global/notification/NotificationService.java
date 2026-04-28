package com.example.spiderbatch.global.notification;

/**
 * @file NotificationService.java
 * @description 배치 완료·실패·SLA 위반 알림 발송 인터페이스.
 *              구현체(Slack, Email)는 SpiderBatchAutoConfiguration에서 Bean으로 등록된다.
 *              알림 발송 실패 시 배치 실행 결과에 영향을 주어서는 안 된다.
 */
public interface NotificationService {

    /**
     * 배치 성공 알림 발송.
     *
     * @param batchAppId     배치 앱 ID (FWK_BATCH_APP.BATCH_APP_ID)
     * @param batchAppName   배치 앱 이름 (표시용)
     * @param writeCount     실제 처리(쓰기) 건수
     * @param elapsedSeconds 소요 시간 (초)
     */
    void sendSuccess(String batchAppId, String batchAppName, long writeCount, long elapsedSeconds);

    /**
     * 배치 실패 알림 발송.
     *
     * @param batchAppId   배치 앱 ID
     * @param batchAppName 배치 앱 이름 (표시용)
     * @param errorReason  오류 사유
     */
    void sendFailure(String batchAppId, String batchAppName, String errorReason);

    /**
     * SLA 초과 에스컬레이션 알림 발송.
     *
     * @param batchAppId     배치 앱 ID
     * @param batchAppName   배치 앱 이름 (표시용)
     * @param elapsedSeconds 실제 소요 시간 (초)
     * @param slaSeconds     FWK_BATCH_APP.SLA_SECONDS — 최대 허용 실행 시간 (초)
     */
    void sendSlaViolation(String batchAppId, String batchAppName, long elapsedSeconds, long slaSeconds);
}
