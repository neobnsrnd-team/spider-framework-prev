package com.example.spiderbatch.global.log;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 배치 실행 감사 로그 기록기.
 *
 * <p>실행 요청·완료·실패 이벤트를 [AUDIT] 태그와 함께 구조화된 형식으로 기록한다.
 * ELK Stack 연동 시 [AUDIT] 태그로 필터링하여 별도 인덱스로 수집 가능하다.</p>
 */
@Slf4j
@Component
public class BatchAuditLogger {

    /**
     * 배치 실행 요청 감사 로그.
     *
     * @param batchAppId 배치 앱 ID
     * @param userId     요청 사용자 ID
     * @param ip         요청 클라이언트 IP
     */
    public void logRequest(String batchAppId, String userId, String ip) {
        log.info("[AUDIT] BATCH_EXEC_REQUEST batchAppId={} userId={} ip={}",
                batchAppId, userId, ip);
    }

    /**
     * 배치 실행 성공 감사 로그.
     *
     * @param batchAppId 배치 앱 ID
     * @param seq        실행 회차
     * @param durationMs Job 실행 소요 시간 (밀리초)
     * @param writeCount StepExecution 기준 실제 쓰기 건수 합산
     */
    public void logSuccess(String batchAppId, int seq, long durationMs, long writeCount) {
        log.info("[AUDIT] BATCH_EXEC_SUCCESS batchAppId={} seq={} durationMs={}ms writeCount={}",
                batchAppId, seq, durationMs, writeCount);
    }

    /**
     * 배치 실행 실패 감사 로그.
     *
     * @param batchAppId 배치 앱 ID
     * @param seq        실행 회차
     * @param reason     실패 사유
     */
    public void logFailure(String batchAppId, int seq, String reason) {
        log.info("[AUDIT] BATCH_EXEC_FAILURE batchAppId={} seq={} reason={}",
                batchAppId, seq, reason);
    }
}
