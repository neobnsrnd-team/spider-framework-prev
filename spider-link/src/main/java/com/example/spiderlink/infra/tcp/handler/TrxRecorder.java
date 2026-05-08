package com.example.spiderlink.infra.tcp.handler;

import com.example.spiderlink.domain.messageinstance.MessageLogQueue;
import com.example.spiderlink.domain.messageinstance.dto.MessageInstanceInsertRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Demo TCP 전문 거래 이력을 FWK_MESSAGE_INSTANCE에 기록하는 컴포넌트.
 *
 * <p>요청(REQ)·응답(RES) 각각 1건씩 INSERT하며 TRX_TRACKING_NO로 쌍을 연결한다.
 * INSERT 실패는 전문 처리 결과에 영향을 주지 않도록 예외를 삼킨다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrxRecorder {

    private static final DateTimeFormatter LOG_FMT  = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final DateTimeFormatter TRX_FMT  = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String ORG_ID      = "DEMO";
    private static final String CHANNEL     = "TCP";
    private static final String RETRY_NO    = "N";
    /** Admin 거래추적로그조회에서 인스턴스 필터로 spider-link 처리 건을 구분하는 식별자 */
    private static final String INSTANCE_ID = "MDW";

    private final MessageLogQueue messageLogQueue;

    /**
     * 요청 전문 이력 INSERT (IO_TYPE=I, REQ_RES_TYPE=Q).
     *
     * @param trxId         거래 ID (예: DEMO_AUTH_LOGIN)
     * @param trxTrackingNo 거래 추적 번호 — REQ·RES 연결 키 (requestId 사용)
     * @param userId        사용자 ID
     * @param messageData   전문 요약 데이터
     */
    public void recordRequest(String trxId, String trxTrackingNo, String userId, String messageData) {
        insertRecord(trxId, trxId + "_REQ", trxTrackingNo, userId, "I", "Q", messageData, "Y");
    }

    /**
     * 응답 전문 이력 INSERT (IO_TYPE=O, REQ_RES_TYPE=S).
     *
     * @param trxId         거래 ID
     * @param trxTrackingNo 거래 추적 번호 — REQ와 동일한 값 사용
     * @param userId        사용자 ID
     * @param messageData   응답 데이터 요약
     * @param successYn     성공 여부 (Y/N)
     */
    public void recordResponse(String trxId, String trxTrackingNo, String userId, String messageData, String successYn) {
        insertRecord(trxId, trxId + "_RES", trxTrackingNo, userId, "O", "S", messageData, successYn);
    }

    private void insertRecord(String trxId, String messageId, String trxTrackingNo, String userId,
                              String ioType, String reqResType, String messageData, String successYn) {
        try {
            LocalDateTime now = LocalDateTime.now();
            messageLogQueue.put(MessageInstanceInsertRequest.builder()
                    .trxId(trxId)
                    .orgId(ORG_ID)
                    .ioType(ioType)
                    .reqResType(reqResType)
                    .messageId(messageId)
                    .trxTrackingNo(truncate(trxTrackingNo != null ? trxTrackingNo.replace("-", "") : null, 30))
                    .userId(userId != null ? userId : "SYSTEM")
                    .logDtime(now.format(LOG_FMT))
                    .lastLogDtime(now.format(LOG_FMT))
                    .lastRtCode("0000")
                    .instanceId(INSTANCE_ID)
                    .retryTrxYn(RETRY_NO)
                    .messageData(truncate(messageData, 4000))
                    .channelType(CHANNEL)
                    .uri(trxId)
                    .successYn(successYn)
                    .trxDtime(now.format(TRX_FMT))
                    .build());
        } catch (Exception e) {
            log.warn("[TrxRecorder] 거래 이력 INSERT 실패: trxId={}, error={}", trxId, e.getMessage());
        }
    }

    private String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }
}
