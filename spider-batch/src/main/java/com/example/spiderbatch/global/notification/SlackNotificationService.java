package com.example.spiderbatch.global.notification;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * @file SlackNotificationService.java
 * @description Slack Incoming Webhook을 통한 배치 알림 구현체.
 *              SLACK_WEBHOOK_URL 환경변수 설정 시 실제 발송, 미설정 시 DEBUG 로그만 남기고 생략한다.
 *              발송 실패는 warn 로그만 기록하고 예외를 전파하지 않는다.
 */
@Slf4j
@Component
public class SlackNotificationService implements NotificationService {

    /** Slack Incoming Webhook URL — SLACK_WEBHOOK_URL 환경변수로 주입 (미설정 시 알림 생략) */
    @Value("${SLACK_WEBHOOK_URL:}")
    private String webhookUrl;

    /** Webhook 호출 전용 RestTemplate — connect/read 5초 타임아웃으로 외부 장애 시 스레드 점유 방지 */
    private static final RestTemplate REST_TEMPLATE = new RestTemplateBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(5))
            .build();

    @Async
    @Override
    public void sendSuccess(String batchAppId, String batchAppName, long writeCount, long elapsedSeconds) {
        String text = String.format("✅ *배치 성공* — %s (%s)%n처리 건수: %,d건 | 소요 시간: %d초",
                batchAppName, batchAppId, writeCount, elapsedSeconds);
        post("good", text);
    }

    @Async
    @Override
    public void sendFailure(String batchAppId, String batchAppName, String errorReason) {
        String text = String.format("❌ *배치 실패* — %s (%s)%n오류: %s",
                batchAppName, batchAppId, errorReason);
        post("danger", text);
    }

    @Async
    @Override
    public void sendSlaViolation(String batchAppId, String batchAppName, long elapsedSeconds, long slaSeconds) {
        // <!channel> 멘션으로 SLA 초과 시 채널 전체 에스컬레이션
        String text = String.format("⚠️ <!channel> *SLA 초과* — %s (%s)%n소요 시간: %d초 (기준: %d초)",
                batchAppName, batchAppId, elapsedSeconds, slaSeconds);
        post("warning", text);
    }

    /**
     * Slack Attachment 포맷으로 Webhook POST 발송.
     *
     * @param color Slack attachment 색상 ("good"=초록, "danger"=빨강, "warning"=노랑)
     * @param text  발송할 메시지 (마크다운 지원)
     */
    private void post(String color, String text) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("SLACK_WEBHOOK_URL 미설정 — Slack 알림 생략: {}", text);
            return;
        }
        try {
            Map<String, Object> attachment = Map.of(
                    "color", color,
                    "text", text,
                    "mrkdwn_in", List.of("text")
            );
            Map<String, Object> payload = Map.of("attachments", List.of(attachment));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            REST_TEMPLATE.postForEntity(webhookUrl, new HttpEntity<>(payload, headers), String.class);
            log.debug("Slack 알림 발송 완료: color={}", color);
        } catch (Exception e) {
            // 알림 실패가 배치 실행 결과에 영향을 주면 안 됨
            log.warn("Slack 알림 발송 실패: {}", e.getMessage());
        }
    }
}
