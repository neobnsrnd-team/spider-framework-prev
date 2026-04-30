package com.example.spideradmin.domain.batch.service;

import com.example.spideradmin.domain.batch.dto.BatchHisCleanupResponse;
import com.example.spideradmin.domain.batch.mapper.BatchHisMapper;
import com.example.spideradmin.domain.batch.mapper.BatchHisPartitionMapper;
import com.example.spideradmin.global.config.SchedulingProperties;
import com.example.spideradmin.global.config.SchedulingProperties.CleanupStrategy;
import com.example.spideradmin.global.exception.InternalException;
import com.example.spideradmin.global.exception.InvalidInputException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 배치 이력 정리 서비스 구현체
 * <p>
 * DELETE 또는 DROP_PARTITION 전략을 지원합니다.
 * </p>
 *
 * <p><b>주의 - DROP_PARTITION 전략:</b></p>
 * <ul>
 *   <li>DDL(ALTER TABLE DROP PARTITION)은 Oracle에서 암묵적 커밋을 수행합니다.</li>
 *   <li>중간에 실패 시 이미 삭제된 파티션은 롤백되지 않습니다.</li>
 *   <li>실패한 파티션부터 재실행하면 정상 동작합니다.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BatchHisCleanupService {

    private final BatchHisMapper batchHisMapper;
    private final BatchHisPartitionMapper batchHisPartitionMapper;
    private final SchedulingProperties schedulingProperties;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    /**
     * 파티션명 패턴 (SQL Injection 방지)
     * 허용 형식: P_YYYYMM (예: P_202401)
     */
    private static final Pattern PARTITION_NAME_PATTERN = Pattern.compile("^P_\\d{6}$");

    private static final Pattern BATCH_DATE_PATTERN = Pattern.compile("^\\d{8}$");

    @Transactional
    public BatchHisCleanupResponse cleanup(int retentionDays) {
        long startTime = System.currentTimeMillis();
        CleanupStrategy strategy = schedulingProperties.getStrategy();

        log.info("[BatchHisCleanup] 정리 시작 - 전략: {}, 보관일수: {}일", strategy, retentionDays);

        BatchHisCleanupResponse result;

        if (strategy == CleanupStrategy.DROP_PARTITION) {
            result = cleanupByDropPartition(retentionDays, startTime);
        } else {
            result = cleanupByDelete(retentionDays, startTime);
        }

        log.info(
                "[BatchHisCleanup] 정리 완료 - 전략: {}, 삭제건수: {}, 소요시간: {}ms",
                result.getStrategy(),
                result.getDeletedCount(),
                result.getElapsedTimeMs());

        return result;
    }

    /**
     * DELETE 전략으로 정리 (기존 방식)
     */
    private BatchHisCleanupResponse cleanupByDelete(int retentionDays, long startTime) {
        String cutoffDate = calculateCutoffDate(retentionDays);

        log.info("[BatchHisCleanup] DELETE 전략 - 기준일: {}", cutoffDate);

        int targetCount = batchHisMapper.countByBatchDateBefore(cutoffDate);
        log.info("[BatchHisCleanup] 삭제 대상 건수: {}", targetCount);

        int deletedCount = 0;
        if (targetCount > 0) {
            deletedCount = batchHisMapper.deleteByBatchDateBefore(cutoffDate);
            log.info("[BatchHisCleanup] DELETE 완료 - {} 건", deletedCount);
        }

        return BatchHisCleanupResponse.builder()
                .cutoffDate(cutoffDate)
                .deletedCount(deletedCount)
                .elapsedTimeMs(System.currentTimeMillis() - startTime)
                .strategy("DELETE")
                .droppedPartitions(Collections.emptyList())
                .build();
    }

    /**
     * DROP PARTITION 전략으로 정리
     * <p>
     * DDL 작업은 암묵적 커밋을 수행하므로 @Transactional 롤백이 불가합니다.
     * 중간 실패 시 이미 삭제된 파티션은 복구되지 않습니다.
     * </p>
     */
    private BatchHisCleanupResponse cleanupByDropPartition(int retentionDays, long startTime) {
        String cutoffDate = calculateCutoffDate(retentionDays);
        String cutoffYearMonth = calculateCutoffYearMonth(retentionDays);

        log.info("[BatchHisCleanup] DROP_PARTITION 전략 - 기준년월: {}", cutoffYearMonth);

        // 파티셔닝 적용 여부 확인
        if (!isPartitioningAvailable()) {
            log.warn("[BatchHisCleanup] 파티셔닝 미적용 테이블입니다. DELETE 전략으로 전환합니다.");
            return cleanupByDelete(retentionDays, startTime);
        }

        // 삭제 대상 파티션 조회
        List<String> partitionsToDelete = batchHisPartitionMapper.selectPartitionsToDelete(cutoffYearMonth);

        if (partitionsToDelete.isEmpty()) {
            log.info("[BatchHisCleanup] 삭제 대상 파티션이 없습니다.");
            return BatchHisCleanupResponse.builder()
                    .cutoffDate(cutoffDate)
                    .deletedCount(0)
                    .elapsedTimeMs(System.currentTimeMillis() - startTime)
                    .strategy("DROP_PARTITION")
                    .droppedPartitions(Collections.emptyList())
                    .build();
        }

        log.info("[BatchHisCleanup] 삭제 대상 파티션: {}", partitionsToDelete);

        // 각 파티션 DROP 실행
        List<String> droppedPartitions = new ArrayList<>();
        long totalEstimatedRows = 0;

        for (String partitionName : partitionsToDelete) {
            // 파티션명 검증 (SQL Injection 방지)
            validatePartitionName(partitionName);

            try {
                // 예상 행 수 조회
                Long estimatedRows = batchHisPartitionMapper.getPartitionRowCount(partitionName);
                if (estimatedRows != null) {
                    totalEstimatedRows += estimatedRows;
                }

                // 파티션 DROP
                batchHisPartitionMapper.dropPartition(partitionName);
                droppedPartitions.add(partitionName);

                log.info("[BatchHisCleanup] 파티션 DROP 완료: {} (예상 {} 건)", partitionName, estimatedRows);

            } catch (Exception e) {
                log.error("[BatchHisCleanup] 파티션 DROP 실패: {} - {}", partitionName, e.getMessage(), e);
                throw new InternalException("partitionName: " + partitionName, e);
            }
        }

        return BatchHisCleanupResponse.builder()
                .cutoffDate(cutoffDate)
                .deletedCount(totalEstimatedRows)
                .elapsedTimeMs(System.currentTimeMillis() - startTime)
                .strategy("DROP_PARTITION")
                .droppedPartitions(droppedPartitions)
                .build();
    }

    public int countDeletionTarget(int retentionDays) {
        String cutoffDate = calculateCutoffDate(retentionDays);
        return batchHisMapper.countByBatchDateBefore(cutoffDate);
    }

    @Transactional
    public int deleteTestTargetHistory(String batchAppId, String instanceId, String batchDate) {
        validateRequiredParam(batchAppId, "batchAppId");
        validateRequiredParam(instanceId, "instanceId");

        String normalizedBatchDate = normalizeBatchDate(batchDate);
        int deletedCount = batchHisMapper.deleteByTargetForTest(batchAppId, instanceId, normalizedBatchDate);

        log.info(
                "[BatchHisCleanup][TEST-ONLY] deletedCount: {}, batchAppId: {}, instanceId: {}, batchDate: {}",
                deletedCount,
                batchAppId,
                instanceId,
                normalizedBatchDate);

        return deletedCount;
    }

    public boolean isPartitioningAvailable() {
        try {
            int partitionCount = batchHisPartitionMapper.countPartitions();
            return partitionCount > 0;
        } catch (Exception e) {
            log.warn("[BatchHisCleanup] 파티션 확인 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 삭제 기준일 계산 (오늘 - 보관일수)
     *
     * @return YYYYMMDD 형식
     */
    private String calculateCutoffDate(int retentionDays) {
        return LocalDate.now().minusDays(retentionDays).format(DATE_FORMATTER);
    }

    /**
     * 삭제 기준 년월 계산 (파티션 매칭용)
     *
     * @return YYYYMM 형식
     */
    private String calculateCutoffYearMonth(int retentionDays) {
        return LocalDate.now().minusDays(retentionDays).format(YEAR_MONTH_FORMATTER);
    }

    /**
     * 파티션명 검증 (SQL Injection 방지)
     * <p>허용 형식: P_YYYYMM (예: P_202401)</p>
     *
     * @throws IllegalArgumentException 유효하지 않은 파티션명
     */
    private void validateRequiredParam(String value, String paramName) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidInputException(paramName + " must not be null or blank.");
        }
    }

    private String normalizeBatchDate(String batchDate) {
        if (batchDate == null || batchDate.trim().isEmpty()) {
            return null;
        }

        if (!BATCH_DATE_PATTERN.matcher(batchDate).matches()) {
            throw new InvalidInputException("batchDate must be in YYYYMMDD format (8 digits).");
        }

        return batchDate;
    }

    /**
     * Validate partition name to prevent SQL injection for dynamic DDL.
     * 1단계: 정규식 검증 (P_YYYYMM 형식)
     * 2단계: DB 실존 확인 (USER_TAB_PARTITIONS Whitelist)
     */
    private void validatePartitionName(String partitionName) {
        if (partitionName == null
                || !PARTITION_NAME_PATTERN.matcher(partitionName).matches()) {
            log.warn("[SECURITY] 유효하지 않은 파티션명 형식 감지: {}", partitionName);
            throw new InvalidInputException("유효하지 않은 파티션명: " + partitionName);
        }

        if (batchHisPartitionMapper.existsPartition(partitionName) == 0) {
            log.warn("[SECURITY] 실존하지 않는 파티션명 감지: {}", partitionName);
            throw new InvalidInputException("존재하지 않는 파티션: " + partitionName);
        }
    }
}
