package com.example.spiderbatch.scheduler;

import com.example.spiderbatch.config.BatchConfigurationProperties;
import com.example.spiderbatch.domain.batch.dto.CronBatchInfo;
import com.example.spiderbatch.domain.batch.mapper.BatchAppMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * WAS 기동 시 Quartz 스케줄 자동 등록.
 *
 * <p>FWK_WAS_EXEC_BATCH JOIN FWK_BATCH_APP 에서 이 인스턴스에 배정되고
 * CRON_TEXT가 설정된 배치를 조회하여 Quartz에 자동 등록한다.</p>
 *
 * <p>RAMJobStore를 사용하므로 WAS 재기동 시마다 재등록이 필요하다.
 * ApplicationRunner를 통해 기동 완료 후 자동으로 실행된다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuartzAutoRegistrar implements ApplicationRunner {

    private final BatchConfigurationProperties batchProps;
    private final BatchAppMapper batchAppMapper;
    private final SchedulerManagementService schedulerManagementService;

    @Override
    public void run(ApplicationArguments args) {
        String instanceId = batchProps.getWas().getInstanceId();
        log.info("[QuartzAutoRegistrar] 스케줄 자동 등록 시작: instanceId={}", instanceId);

        List<CronBatchInfo> cronBatches = batchAppMapper.selectCronBatchesByInstanceId(instanceId);

        if (cronBatches.isEmpty()) {
            log.info("[QuartzAutoRegistrar] 등록할 Cron 배치 없음: instanceId={}", instanceId);
            return;
        }

        int successCount = 0;
        for (CronBatchInfo batch : cronBatches) {
            try {
                schedulerManagementService.registerJob(batch.getBatchAppId(), batch.getCronText());
                successCount++;
            } catch (Exception e) {
                // 개별 Job 등록 실패가 전체 기동을 막지 않도록 예외를 잡아 로그만 기록
                log.error("[QuartzAutoRegistrar] Job 등록 실패, 기동 계속: batchAppId={}, cron={}",
                        batch.getBatchAppId(), batch.getCronText(), e);
            }
        }

        log.info("[QuartzAutoRegistrar] 스케줄 자동 등록 완료: {}/{}건", successCount, cronBatches.size());
    }
}
