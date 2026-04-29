package com.example.spider_admin.domain.batch.scheduler;

import com.example.spider_admin.domain.batch.dto.BatchHisCleanupResponse;
import com.example.spider_admin.domain.batch.service.BatchHisCleanupService;
import com.example.spider_admin.global.config.SchedulingProperties;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * 배치 실행 이력 정리 스케줄러
 * <p>
 * 매일 새벽 2시에 실행되어 보관 기간(기본 90일) 경과 데이터를 삭제합니다.
 * </p>
 *
 * <p>설정:</p>
 * <ul>
 *   <li>scheduling.batch-his-cleanup.enabled: 스케줄러 활성화 여부 (필수 설정)</li>
 *   <li>scheduling.batch-his-cleanup.cron: 실행 스케줄 (기본: 매일 02:00)</li>
 *   <li>scheduling.batch-his-cleanup.retention-days: 보관 기간 (기본: 90일)</li>
 *   <li>scheduling.batch-his-cleanup.strategy: 정리 전략 (DELETE 또는 DROP_PARTITION)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "scheduling.batch-his-cleanup",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false)
public class BatchHisCleanupScheduler {

    private final BatchHisCleanupService batchHisCleanupService;
    private final SchedulingProperties schedulingProperties;

    private static final DateTimeFormatter LOG_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 배치 이력 정리 스케줄 실행
     * <p>Cron 표현식: 매일 새벽 2시 (0 0 2 * * ?)</p>
     */
    // TODO: 추후 다같이 재검토
    // @Scheduled(cron = "${scheduling.batch-his-cleanup.cron:0 0 2 * * ?}")
    public void cleanupBatchHistory() {
        String startTime = LocalDateTime.now().format(LOG_FORMATTER);
        String strategy = schedulingProperties.getStrategy().name();

        log.info("[BatchHisCleanup] 스케줄 시작 - 시간: {}, 전략: {}", startTime, strategy);

        try {
            int retentionDays = schedulingProperties.getRetentionDays();

            BatchHisCleanupResponse result = batchHisCleanupService.cleanup(retentionDays);

            // 전략에 따른 로깅
            if (result.getDroppedPartitions() != null
                    && !result.getDroppedPartitions().isEmpty()) {
                log.info(
                        "[BatchHisCleanup] 스케줄 완료 - 전략: {}, 기준일: {}, " + "삭제 파티션: {}, 예상 건수: {}, 소요시간: {}ms",
                        result.getStrategy(),
                        result.getCutoffDate(),
                        result.getDroppedPartitions(),
                        result.getDeletedCount(),
                        result.getElapsedTimeMs());
            } else {
                log.info(
                        "[BatchHisCleanup] 스케줄 완료 - 전략: {}, 기준일: {}, " + "삭제 건수: {}, 소요시간: {}ms",
                        result.getStrategy(),
                        result.getCutoffDate(),
                        result.getDeletedCount(),
                        result.getElapsedTimeMs());
            }

        } catch (DataAccessException e) {
            log.error("[BatchHisCleanup] 데이터베이스 작업 실패 - {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("[BatchHisCleanup] 예상치 못한 스케줄 실패 - {}", e.getMessage(), e);
        }
    }
}
