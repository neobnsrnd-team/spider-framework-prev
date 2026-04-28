package com.example.spiderbatch.global.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * @file EmailNotificationService.java
 * @description Email을 통한 배치 알림 구현체.
 *              spring.mail.host(SMTP_HOST)가 설정된 경우 JavaMailSender를 통해 실 발송,
 *              미설정 시 로그 출력으로 폴백(Mock 동작)하여 코드 변경 없이 SMTP 전환이 가능하다.
 */
@Slf4j
@Component
public class EmailNotificationService implements NotificationService {

    /**
     * JavaMailSender — spring.mail.host(SMTP_HOST) 설정 시 Spring Boot Auto-Configuration이 등록.
     * 미설정 시 null로 남아 Mock(로그 출력) 모드로 동작한다.
     */
    @Autowired(required = false)
    private JavaMailSender mailSender;

    /** 수신 이메일 주소 — NOTIFICATION_EMAIL_TO 환경변수로 주입 (미설정 시 알림 생략) */
    @Value("${notification.email.to:}")
    private String emailTo;

    @Override
    public void sendSuccess(String batchAppId, String batchAppName, long writeCount, long elapsedSeconds) {
        String subject = String.format("[배치 성공] %s (%s)", batchAppName, batchAppId);
        String body = String.format(
                "배치가 성공적으로 완료되었습니다.%n%n배치 앱 ID: %s%n배치 앱 이름: %s%n처리 건수: %,d건%n소요 시간: %d초",
                batchAppId, batchAppName, writeCount, elapsedSeconds);
        send(subject, body);
    }

    @Override
    public void sendFailure(String batchAppId, String batchAppName, String errorReason) {
        String subject = String.format("[배치 실패] %s (%s)", batchAppName, batchAppId);
        String body = String.format(
                "배치가 실패하였습니다.%n%n배치 앱 ID: %s%n배치 앱 이름: %s%n오류 사유: %s",
                batchAppId, batchAppName, errorReason);
        send(subject, body);
    }

    @Override
    public void sendSlaViolation(String batchAppId, String batchAppName, long elapsedSeconds, long slaSeconds) {
        String subject = String.format("[SLA 초과] %s (%s)", batchAppName, batchAppId);
        String body = String.format(
                "배치 실행 시간이 SLA 기준을 초과하였습니다.%n%n배치 앱 ID: %s%n배치 앱 이름: %s%n소요 시간: %d초%n기준 시간: %d초",
                batchAppId, batchAppName, elapsedSeconds, slaSeconds);
        send(subject, body);
    }

    private void send(String subject, String body) {
        if (mailSender == null) {
            // SMTP 미설정 시 Mock 모드 — .env에 SMTP_HOST 등을 추가하면 실 발송으로 자동 전환
            log.info("[Email Mock] SMTP 미설정 — 알림 로그 출력: subject={}", subject);
            return;
        }
        if (emailTo == null || emailTo.isBlank()) {
            log.warn("NOTIFICATION_EMAIL_TO 미설정 — Email 알림 생략: subject={}", subject);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(emailTo);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email 알림 발송 완료: to={}, subject={}", emailTo, subject);
        } catch (Exception e) {
            // 알림 실패가 배치 실행 결과에 영향을 주면 안 됨
            log.warn("Email 알림 발송 실패: subject={}, error={}", subject, e.getMessage());
        }
    }
}
