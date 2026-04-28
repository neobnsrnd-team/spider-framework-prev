package com.example.spiderbatch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.stereotype.Service;

/**
 * Quartz 스케줄러 관리 서비스.
 *
 * <p>Job 등록·수정·삭제·즉시 실행을 담당한다.
 * Job 이름은 batchAppId, Group은 {@value JOB_GROUP}으로 고정한다.</p>
 *
 * <p>현재 JobStore는 RAMJobStore(in-memory)를 사용한다.
 * TODO: 운영 환경에서는 JDBC JobStore로 전환하여 WAS 재기동 시 스케줄을 복구할 것.
 *       application.yml: {@code spring.quartz.job-store-type: jdbc}</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerManagementService {

    private final Scheduler scheduler;

    static final String JOB_GROUP = "BATCH";

    /**
     * 배치 Job을 Quartz에 등록한다.
     * 이미 동일 Job이 존재하면 Trigger를 새 cronText로 교체한다.
     *
     * @param batchAppId 배치 APP ID (Job 이름으로 사용)
     * @param cronText   Quartz Cron 표현식 (예: {@code 0 0 2 * * ?})
     */
    public void registerJob(String batchAppId, String cronText) {
        try {
            JobKey jobKey = JobKey.jobKey(batchAppId, JOB_GROUP);

            if (scheduler.checkExists(jobKey)) {
                // 이미 등록된 Job — Trigger만 교체
                reschedule(batchAppId, cronText);
                return;
            }

            JobDetail jobDetail = JobBuilder.newJob(BatchJobQuartzTrigger.class)
                    .withIdentity(jobKey)
                    .usingJobData("batchAppId", batchAppId)
                    // durability: true — Trigger 없이도 Job을 보존 (즉시 실행 후 재등록 가능)
                    .storeDurably(true)
                    .build();

            CronTrigger trigger = buildCronTrigger(batchAppId, cronText);
            scheduler.scheduleJob(jobDetail, trigger);
            log.info("[Quartz] Job 등록: batchAppId={}, cron={}", batchAppId, cronText);

        } catch (SchedulerException e) {
            log.error("[Quartz] Job 등록 실패: batchAppId={}, cron={}", batchAppId, cronText, e);
            throw new RuntimeException("Quartz Job 등록 실패: " + batchAppId, e);
        }
    }

    /**
     * 기존 Job의 Cron 표현식을 변경한다.
     * Job이 존재하지 않으면 새로 등록한다.
     *
     * @param batchAppId 배치 APP ID
     * @param cronText   새 Cron 표현식
     */
    public void reschedule(String batchAppId, String cronText) {
        try {
            JobKey jobKey = JobKey.jobKey(batchAppId, JOB_GROUP);

            if (!scheduler.checkExists(jobKey)) {
                // Job이 없으면 신규 등록으로 전환
                registerJob(batchAppId, cronText);
                return;
            }

            TriggerKey triggerKey = TriggerKey.triggerKey(batchAppId, JOB_GROUP);
            CronTrigger newTrigger = buildCronTrigger(batchAppId, cronText);
            // rescheduleJob이 null을 반환하면 Trigger가 없는 상태 — 새로 scheduleJob
            if (scheduler.rescheduleJob(triggerKey, newTrigger) == null) {
                scheduler.scheduleJob(newTrigger);
                log.info("[Quartz] Trigger 없음 — 재생성: batchAppId={}, cron={}", batchAppId, cronText);
            } else {
                log.info("[Quartz] 스케줄 변경: batchAppId={}, cron={}", batchAppId, cronText);
            }

        } catch (SchedulerException e) {
            log.error("[Quartz] 스케줄 변경 실패: batchAppId={}, cron={}", batchAppId, cronText, e);
            throw new RuntimeException("Quartz 스케줄 변경 실패: " + batchAppId, e);
        }
    }

    /**
     * Quartz에 등록된 배치 Job을 삭제한다.
     *
     * @param batchAppId 배치 APP ID
     */
    public void deleteJob(String batchAppId) {
        try {
            JobKey jobKey = JobKey.jobKey(batchAppId, JOB_GROUP);
            boolean deleted = scheduler.deleteJob(jobKey);
            if (deleted) {
                log.info("[Quartz] Job 삭제: batchAppId={}", batchAppId);
            } else {
                log.warn("[Quartz] 삭제할 Job 없음: batchAppId={}", batchAppId);
            }
        } catch (SchedulerException e) {
            log.error("[Quartz] Job 삭제 실패: batchAppId={}", batchAppId, e);
            throw new RuntimeException("Quartz Job 삭제 실패: " + batchAppId, e);
        }
    }

    /**
     * Quartz에 등록된 배치 Job을 즉시 실행한다.
     * Job이 등록되어 있지 않아도 실행 가능 — 등록 후 triggerJob 호출.
     *
     * @param batchAppId 배치 APP ID
     * @param batchDate  배치 기준일 (null이면 Job 내부에서 당일로 자동 설정)
     */
    public void triggerNow(String batchAppId, String batchDate) {
        try {
            JobKey jobKey = JobKey.jobKey(batchAppId, JOB_GROUP);

            // replace=true: 동시 요청 race condition 방어 — 이미 존재해도 덮어쓰지 않고 정상 처리
            if (!scheduler.checkExists(jobKey)) {
                JobDetail jobDetail = JobBuilder.newJob(BatchJobQuartzTrigger.class)
                        .withIdentity(jobKey)
                        .usingJobData("batchAppId", batchAppId)
                        .storeDurably(true)
                        .build();
                scheduler.addJob(jobDetail, true);
            }

            // batchDate를 JobDataMap으로 전달 — BatchJobQuartzTrigger에서 manualBatchDate 키로 읽음
            JobDataMap dataMap = new JobDataMap();
            if (batchDate != null && !batchDate.isBlank()) {
                dataMap.put("manualBatchDate", batchDate);
            }
            scheduler.triggerJob(jobKey, dataMap);
            log.info("[Quartz] 즉시 실행 트리거: batchAppId={}, batchDate={}", batchAppId, batchDate);

        } catch (SchedulerException e) {
            log.error("[Quartz] 즉시 실행 실패: batchAppId={}", batchAppId, e);
            throw new RuntimeException("Quartz 즉시 실행 실패: " + batchAppId, e);
        }
    }

    /** CronTrigger 생성 헬퍼 */
    private CronTrigger buildCronTrigger(String batchAppId, String cronText) {
        return TriggerBuilder.newTrigger()
                .withIdentity(TriggerKey.triggerKey(batchAppId, JOB_GROUP))
                .withSchedule(CronScheduleBuilder.cronSchedule(cronText)
                        // 미스파이어: 다음 Cron 주기에 실행 (misfire 무시 — 적체 방지)
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }
}
