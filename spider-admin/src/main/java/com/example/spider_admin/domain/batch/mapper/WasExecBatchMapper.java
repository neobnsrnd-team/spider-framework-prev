package com.example.spider_admin.domain.batch.mapper;

import com.example.spider_admin.domain.batch.dto.WasExecBatchResponse;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * WasExecBatch Mapper (M:N 관계)
 * BATCH_APP과 WAS_INSTANCE의 교차 테이블 CRUD 담당
 */
public interface WasExecBatchMapper {

    // 기본 CRUD
    void insertWasExecBatch(
            @Param("batchAppId") String batchAppId,
            @Param("instanceId") String instanceId,
            @Param("useYn") String useYn,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    void updateWasExecBatch(
            @Param("batchAppId") String batchAppId,
            @Param("instanceId") String instanceId,
            @Param("useYn") String useYn,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    void deleteWasExecBatchById(@Param("batchAppId") String batchAppId, @Param("instanceId") String instanceId);

    // 조회
    List<WasExecBatchResponse> selectByBatchAppIdWithDetails(String batchAppId);

    WasExecBatchResponse selectById(@Param("batchAppId") String batchAppId, @Param("instanceId") String instanceId);

    // 존재 확인
    int countById(@Param("batchAppId") String batchAppId, @Param("instanceId") String instanceId);

    // 일괄 삭제
    void deleteByBatchAppId(String batchAppId);

    // 인스턴스 삭제 시 cascade
    void deleteByInstanceId(String instanceId);

    // Batch 작업 (Oracle UNION ALL 패턴)
    void insertWasExecBatchBatch(@Param("list") List<Map<String, String>> list);

    /**
     * 해당 배치에 배정된 활성(USE_YN='Y') WAS 인스턴스 ID 목록 조회.
     * 스케줄 Cron 변경 시 모든 배정 인스턴스에 TCP 커맨드를 전송하기 위해 사용한다.
     *
     * @param batchAppId 배치 APP ID
     * @return USE_YN='Y' 인스턴스 ID 목록
     */
    List<String> selectActiveInstanceIds(@Param("batchAppId") String batchAppId);
}
