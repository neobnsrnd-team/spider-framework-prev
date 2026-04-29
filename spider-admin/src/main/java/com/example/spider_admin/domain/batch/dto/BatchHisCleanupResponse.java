package com.example.spider_admin.domain.batch.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 배치 이력 정리 결과 DTO
 */
@Getter
@Builder
public class BatchHisCleanupResponse {

    /**
     * 삭제 기준일 (YYYYMMDD)
     */
    private final String cutoffDate;

    /**
     * 삭제된 건수 (DELETE 전략) 또는 예상 행 수 (DROP_PARTITION 전략)
     */
    private final long deletedCount;

    /**
     * 소요 시간 (밀리초)
     */
    private final long elapsedTimeMs;

    /**
     * 사용된 정리 전략
     */
    private final String strategy;

    /**
     * 삭제된 파티션 목록 (DROP_PARTITION 전략에서만 사용)
     */
    private final List<String> droppedPartitions;
}
