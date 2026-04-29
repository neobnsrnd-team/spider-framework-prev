package com.example.spider_admin.domain.batch.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * FWK_BATCH_HIS 파티션 관리 Mapper
 * <p>
 * 파티션 메타데이터 조회 및 DDL 작업을 담당합니다.
 * DROP PARTITION 전략 사용 시 BatchHisCleanupService에서 호출됩니다.
 * </p>
 *
 * <p><b>주의:</b> DDL 작업(dropPartition)은 Oracle에서 암묵적 커밋을 수행합니다.
 * 따라서 트랜잭션 롤백이 불가능합니다.</p>
 */
public interface BatchHisPartitionMapper {

    /**
     * FWK_BATCH_HIS 테이블의 파티션 개수 조회
     * <p>파티셔닝 적용 여부 확인용</p>
     *
     * @return 파티션 개수 (0이면 파티셔닝 미적용)
     */
    int countPartitions();

    /**
     * 삭제 대상 파티션 목록 조회
     * <p>파티션 명명 규칙: P_YYYYMM (예: P_202401)</p>
     *
     * @param cutoffYearMonth 기준 년월 (YYYYMM 형식) - 이 년월 미만 파티션 조회
     * @return 삭제 대상 파티션명 목록
     */
    List<String> selectPartitionsToDelete(@Param("cutoffYearMonth") String cutoffYearMonth);

    /**
     * 파티션 DROP 실행
     * <p>
     * ALTER TABLE FWK_BATCH_HIS DROP PARTITION {partitionName} 실행
     * </p>
     * <p><b>주의:</b> DDL 작업이므로 자동 커밋됩니다.</p>
     *
     * @param partitionName 삭제할 파티션명 (예: P_202401)
     */
    void dropPartition(@Param("partitionName") String partitionName);

    /**
     * 파티션의 예상 행 수 조회
     * <p>통계 정보 기반 (DBMS_STATS 수집 필요)</p>
     *
     * @param partitionName 파티션명
     * @return 예상 행 수 (통계 미수집 시 null)
     */
    Long getPartitionRowCount(@Param("partitionName") String partitionName);

    /**
     * 파티션 실존 여부 확인 (SQL Injection 방어용 Whitelist 검증)
     *
     * @param partitionName 파티션명
     * @return 존재하면 1 이상, 미존재 시 0
     */
    int existsPartition(@Param("partitionName") String partitionName);
}
