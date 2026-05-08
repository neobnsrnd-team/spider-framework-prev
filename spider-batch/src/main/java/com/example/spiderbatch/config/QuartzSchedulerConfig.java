package com.example.spiderbatch.config;

import com.example.spiderbatch.domain.batch.mapper.BatchAppMapper;
import com.example.spiderbatch.scheduler.BatchJobQuartzTrigger;
import com.example.spiderbatch.scheduler.QuartzAutoRegistrar;
import com.example.spiderbatch.scheduler.SchedulerManagementService;
import com.example.spiderbatch.tcp.ScheduleCommandHandler;
import org.quartz.Scheduler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Quartz 스케줄러 자동 설정.
 *
 * <p>{@code spring-boot-starter-quartz}가 클래스패스에 있을 때만 활성화된다.
 * 스케줄 관리 서비스, 기동 시 자동 등록, TCP 스케줄 커맨드 핸들러를 등록한다.</p>
 *
 * <p>Quartz 미포함 환경에서는 이 설정 전체가 스킵된다.
 * {@link BatchJobQuartzTrigger}는 Quartz가 직접 인스턴스화하므로 Bean 등록이 불필요하다.</p>
 */
@Configuration
@ConditionalOnClass(Scheduler.class)
public class QuartzSchedulerConfig {

    /**
     * Quartz Job 등록·수정·삭제·즉시 실행을 담당하는 서비스.
     */
    @Bean
    @ConditionalOnMissingBean
    public SchedulerManagementService schedulerManagementService(Scheduler scheduler) {
        return new SchedulerManagementService(scheduler);
    }

    /**
     * WAS 기동 시 DB에서 Cron 배치 목록을 조회하여 Quartz에 자동 등록하는 ApplicationRunner.
     */
    @Bean
    @ConditionalOnMissingBean
    public QuartzAutoRegistrar quartzAutoRegistrar(
            BatchConfigurationProperties batchProps,
            BatchAppMapper batchAppMapper,
            SchedulerManagementService schedulerManagementService) {
        return new QuartzAutoRegistrar(batchProps, batchAppMapper, schedulerManagementService);
    }

    /**
     * Admin TCP SCHEDULE_TRIGGER / SCHEDULE_CRON_UPDATE 커맨드 핸들러.
     */
    @Bean
    @ConditionalOnMissingBean
    public ScheduleCommandHandler scheduleCommandHandler(SchedulerManagementService schedulerManagementService) {
        return new ScheduleCommandHandler(schedulerManagementService);
    }
}
