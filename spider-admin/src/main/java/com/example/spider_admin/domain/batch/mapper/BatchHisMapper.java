package com.example.spider_admin.domain.batch.mapper;

import com.example.spider_admin.domain.batch.dto.BatchHisResponse;
import com.example.spider_admin.domain.batch.dto.BatchHisSearchRequest;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * 배치 실행 이력 Mapper (CRUD + Query)
 */
public interface BatchHisMapper {

    // ==================== 생성 ====================

    /**
     * 배치 실행 이력 등록 (개별 파라미터)
     */
    void insertBatchHis(
            @Param("batchAppId") String batchAppId,
            @Param("instanceId") String instanceId,
            @Param("batchDate") String batchDate,
            @Param("batchExecuteSeq") Integer batchExecuteSeq,
            @Param("logDtime") String logDtime,
            @Param("resRtCode") String resRtCode,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * 배치 실행 이력 일괄 등록 (Oracle UNION ALL 패턴)
     * <p>다수의 인스턴스에 대해 배치 실행 요청 시 성능 최적화</p>
     *
     * @param list 등록할 배치 실행 이력 목록 (Map 형태)
     */
    void insertBatchHisBatch(@Param("list") List<Map<String, Object>> list);

    // ==================== 상태 업데이트 ====================

    /**
     * 배치 실행 결과 상태 업데이트
     * <p>TODO: WAS 측 배치 실행 API 구현 후, 이 UPDATE는 WAS에서 직접 수행하도록 이관</p>
     */
    void updateBatchHisResult(
            @Param("batchAppId") String batchAppId,
            @Param("instanceId") String instanceId,
            @Param("batchDate") String batchDate,
            @Param("batchExecuteSeq") Integer batchExecuteSeq,
            @Param("resRtCode") String resRtCode,
            @Param("batchEndDtime") String batchEndDtime,
            @Param("errorReason") String errorReason,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    // ==================== 실행 가드 ====================

    int countExecutingByBatchAppId(@Param("batchAppId") String batchAppId);

    String selectLatestStatusByBatchAppId(@Param("batchAppId") String batchAppId);

    // ==================== 단건 조회 ====================

    /**
     * 특정 일자/인스턴스의 다음 실행 순번 조회
     */
    int selectNextExecuteSeq(
            @Param("batchAppId") String batchAppId,
            @Param("instanceId") String instanceId,
            @Param("batchDate") String batchDate);

    // ==================== 삭제 (이력 정리용) ====================

    /**
     * 특정 날짜 이전 배치 이력 삭제
     *
     * @param batchDate 기준일 (YYYYMMDD) - 이 날짜 미만 데이터 삭제
     * @return 삭제된 건수
     */
    int deleteByBatchDateBefore(@Param("batchDate") String batchDate);

    /**
     * 특정 날짜 이전 배치 이력 건수 조회 (삭제 전 확인용)
     *
     * @param batchDate 기준일 (YYYYMMDD)
     * @return 삭제 대상 건수
     */
    int countByBatchDateBefore(@Param("batchDate") String batchDate);

    /**
     * TEST-ONLY: 특정 배치 앱/인스턴스(선택: 배치일자)의 이력 삭제
     *
     * @param batchAppId 배치 앱 ID (필수)
     * @param instanceId 인스턴스 ID (필수)
     * @param batchDate 배치 일자(YYYYMMDD, 선택)
     * @return 삭제 건수
     */
    int deleteByTargetForTest(
            @Param("batchAppId") String batchAppId,
            @Param("instanceId") String instanceId,
            @Param("batchDate") String batchDate);

    // ==================== 목록 조회 ====================

    /**
     * 배치 실행 이력 목록 조회 (검색 + 페이지네이션)
     */
    List<BatchHisResponse> findAllWithSearch(
            @Param("search") BatchHisSearchRequest search, @Param("offset") int offset, @Param("endRow") int endRow);

    /**
     * 배치 실행 이력 건수 조회
     */
    long countAllWithSearch(@Param("search") BatchHisSearchRequest search);

    /**
     * 배치 실행 이력 단건 조회 (조인 포함)
     */
    BatchHisResponse findByIdWithDetails(
            @Param("batchAppId") String batchAppId,
            @Param("instanceId") String instanceId,
            @Param("batchDate") String batchDate,
            @Param("batchExecuteSeq") Integer batchExecuteSeq);

    /**
     * 배치 실행 이력 전체 조회 (엑셀 내보내기용)
     */
    List<BatchHisResponse> findAllForExport(@Param("search") BatchHisSearchRequest search);
}
