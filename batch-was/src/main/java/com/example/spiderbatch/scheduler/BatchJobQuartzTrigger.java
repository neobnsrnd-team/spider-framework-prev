package com.example.spiderbatch.scheduler;

import com.example.spiderbatch.domain.batch.dto.BatchExecuteRequest;
import com.example.spiderbatch.domain.batch.service.BatchExecuteService;
import com.example.spiderbatch.lock.RedisDistributedLockService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Quartz Job 구현체 — 배치 자동 실행 트리거.
 *
 * <p>Quartz Scheduler가 Cron 주기에 맞춰 이 Job을 실행하면:
 * <ol>
 *   <li>Redis 분산 락 획득 시도 — 실패 시(다른 인스턴스가 실행 중) 스킵</li>
 *   <li>{@link BatchExecuteService#execute}로 배치 실행 위임</li>
 *   <li>finally에서 분산 락 반드시 해제</li>
 * </ol>
 * </p>
 *
 * <p>JobDataMap 필수 키: {@code batchAppId}</p>
 */
@Slf4j
public class BatchJobQuartzTrigger extends QuartzJobBean {

    /** QuartzJobBean은 Spring DI(@Autowired)를 지원한다 */
    @Autowired
    private BatchExecuteService batchExecuteService;

    @Autowired
    private RedisDistributedLockService lockService;

    private static final DateTimeFormatter BATCH_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** Quartz가 스케줄에 따라 호출하는 실행 메서드 */
    @Override
    protected void executeInternal(JobExecutionContext ctx) {
        String batchAppId = ctx.getMergedJobDataMap().getString("batchAppId");
        if (batchAppId == null || batchAppId.isBlank()) {
            log.error("[Quartz] JobDataMap에 batchAppId 없음 — 실행 불가: jobKey={}", ctx.getJobDetail().getKey());
            return;
        }

        log.info("[Quartz] 배치 실행 시작: batchAppId={}", batchAppId);

        if (!lockService.tryLock(batchAppId)) {
            // 다른 WAS 인스턴스가 동일 배치를 실행 중 — 정상 스킵
            log.info("[Quartz] 분산 락 획득 실패, 실행 스킵: batchAppId={}", batchAppId);
            return;
        }

        try {
            // Admin 즉시 실행 시 전달된 날짜 우선, 없으면 실행 당일
            String manualBatchDate = ctx.getMergedJobDataMap().getString("manualBatchDate");
            BatchExecuteRequest request = BatchExecuteRequest.builder()
                    .batchAppId(batchAppId)
                    .batchDate(manualBatchDate != null ? manualBatchDate : LocalDate.now().format(BATCH_DATE_FMT))
                    .userId("QUARTZ_SCHEDULER")
                    .build();

            batchExecuteService.execute(request, "QUARTZ_INTERNAL");
            log.info("[Quartz] 배치 실행 완료: batchAppId={}", batchAppId);

        } catch (Exception e) {
            log.error("[Quartz] 배치 실행 중 예외: batchAppId={}", batchAppId, e);
        } finally {
            lockService.unlock(batchAppId);
        }
    }
}
