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
 * 배치 Job 완료·실패 시 Slack·Email 알림을 발송하는 {@link JobExecutionListener}.
 *
 * <p>알림 로직을 Job과 분리하여 여러 Job 설정 클래스에서 공통으로 재사용한다.
 * {@link com.example.spiderbatch.config.SpiderBatchAutoConfiguration}에 의해 자동 등록되며,
 * Job 설정 클래스에서 의존성으로 주입받아 {@code .listener(batchNotificationListener)}로 연결한다.</p>
 *
 * <p>batchAppName은 {@link com.example.spiderbatch.domain.batch.service.BatchExecuteService}가
 * JobParameter에 포함시켜 전달한다. 없으면 batchAppId로 대체한다.</p>
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

        String batchAppId = jobExecution.getJobParameters().getString("batchAppId", "UNKNOWN");
        // batchAppName은 BatchExecuteService가 JobParameter에 포함시켜 전달 (없으면 ID로 대체)
        String batchAppName = jobExecution.getJobParameters().getString("batchAppName", batchAppId);

        long writeCount = jobExecution.getStepExecutions().stream()
                .mapToLong(StepExecution::getWriteCount)
                .sum();
        long elapsedSeconds = calcElapsedSeconds(jobExecution);

        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            notificationServices.forEach(svc ->
                    svc.sendSuccess(batchAppId, batchAppName, writeCount, elapsedSeconds));
        } else {
            String errorReason = collectErrorReason(jobExecution);
            notificationServices.forEach(svc ->
                    svc.sendFailure(batchAppId, batchAppName, errorReason));
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
