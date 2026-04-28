package com.example.spiderbatch.job.listener;

import com.example.spiderbatch.global.notification.NotificationService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

/**
 * @file BatchNotificationListener.java
 * @description 배치 Job 완료·실패 시 Slack·Email 알림을 발송하는 JobExecutionListener.
 *              알림 로직을 Job과 분리하여 4개 샘플 Job에 공통 적용한다.
 *              batchAppName은 JobParameters의 batchAppId를 그대로 사용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchNotificationListener implements JobExecutionListener {

    private final List<NotificationService> notificationServices;

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (notificationServices.isEmpty()) {
            return;
        }

        // batchAppId를 알림 표시명으로 사용 (ex. FILE2DB_JOB, DB2DB_JOB)
        String batchAppId = jobExecution.getJobParameters().getString("batchAppId", "UNKNOWN");

        long writeCount = jobExecution.getStepExecutions().stream()
                .mapToLong(StepExecution::getWriteCount)
                .sum();
        long elapsedSeconds = calcElapsedSeconds(jobExecution);

        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            notificationServices.forEach(svc ->
                    svc.sendSuccess(batchAppId, batchAppId, writeCount, elapsedSeconds));
        } else {
            String errorReason = collectErrorReason(jobExecution);
            notificationServices.forEach(svc ->
                    svc.sendFailure(batchAppId, batchAppId, errorReason));
        }
    }

    private long calcElapsedSeconds(JobExecution jobExecution) {
        LocalDateTime start = jobExecution.getStartTime();
        LocalDateTime end = jobExecution.getEndTime();
        if (start == null || end == null) {
            return 0L;
        }
        return Duration.between(start, end).getSeconds();
    }

    private String collectErrorReason(JobExecution jobExecution) {
        return jobExecution.getAllFailureExceptions().stream()
                .map(Throwable::getMessage)
                .filter(msg -> msg != null && !msg.isBlank())
                .findFirst()
                .orElse("BatchStatus=" + jobExecution.getStatus());
    }
}
