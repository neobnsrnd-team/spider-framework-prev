package com.example.spideradmin.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 배치 이력 정리 스케줄러 설정 프로퍼티
 */
@Component
@ConfigurationProperties(prefix = "scheduling.batch-his-cleanup")
@Getter
@Setter
public class SchedulingProperties {

    /**
     * 스케줄러 활성화 여부
     */
    private boolean enabled = true;

    /**
     * 스케줄 실행 Cron 표현식 (기본: 매일 새벽 2시)
     */
    private String cron = "0 0 2 * * ?";

    /**
     * 데이터 보관 기간 (일)
     */
    private int retentionDays = 90;

    /**
     * 정리 전략
     * <ul>
     *   <li>DELETE: 행 단위 삭제 (파티셔닝 미적용 환경)</li>
     *   <li>DROP_PARTITION: 파티션 DROP (파티셔닝 적용 환경, 권장)</li>
     * </ul>
     */
    private CleanupStrategy strategy = CleanupStrategy.DELETE;

    /**
     * 정리 전략 enum
     */
    public enum CleanupStrategy {
        /**
         * 행 단위 DELETE (기존 방식)
         */
        DELETE,

        /**
         * 파티션 DROP (Oracle 파티셔닝 적용 시)
         */
        DROP_PARTITION
    }
}
