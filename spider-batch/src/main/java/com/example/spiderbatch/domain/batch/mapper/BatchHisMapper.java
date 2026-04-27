package com.example.spiderbatch.domain.batch.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * FWK_BATCH_HIS 이력 관리 Mapper.
 *
 * <p>WAS가 배치 이력을 직접 INSERT/UPDATE한다.
 * Admin의 TODO 주석에서 이관된 책임이다.</p>
 */
@Mapper
public interface BatchHisMapper {

    /**
     * 같은 날, 같은 배치, 같은 인스턴스의 다음 실행 회차 조회.
     * NVL(MAX(BATCH_EXECUTE_SEQ), 0) + 1 패턴.
     *
     * @param batchAppId 배치 APP ID
     * @param instanceId WAS 인스턴스 ID
     * @param batchDate  배치 기준일 (YYYYMMDD)
     * @return 다음 실행 회차 (첫 실행이면 1)
     */
    int selectNextExecuteSeq(
            @Param("batchAppId") String batchAppId,
            @Param("instanceId") String instanceId,
            @Param("batchDate") String batchDate);

    /**
     * 배치 시작 이력 INSERT (RES_RT_CODE = '0', STARTED).
     *
     * @param batchAppId      배치 APP ID
     * @param instanceId      WAS 인스턴스 ID
     * @param batchDate       배치 기준일
     * @param batchExecuteSeq 실행 회차
     * @param logDtime        시작 일시
     * @param lastUpdateUserId 실행자 ID
     */
    void insertBatchHis(
            @Param("batchAppId") String batchAppId,
            @Param("instanceId") String instanceId,
            @Param("batchDate") String batchDate,
            @Param("batchExecuteSeq") int batchExecuteSeq,
            @Param("logDtime") String logDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * 배치 실행 결과 UPDATE.
     *
     * @param batchAppId       배치 APP ID
     * @param instanceId       WAS 인스턴스 ID
     * @param batchDate        배치 기준일
     * @param batchExecuteSeq  실행 회차
     * @param resRtCode        결과 코드 (1: SUCCESS, 9: ABNORMAL_TERMINATION)
     * @param batchEndDtime    종료 일시
     * @param errorReason      오류 사유 (정상 종료 시 null)
     * @param executeCount     총 처리 건수
     * @param successCount     성공 건수
     * @param failCount        실패 건수
     * @param lastUpdateUserId 실행자 ID
     */
    /** UPDATE 성공 시 1, 일치하는 PK가 없으면 0 반환 */
    int updateBatchHisResult(
            @Param("batchAppId") String batchAppId,
            @Param("instanceId") String instanceId,
            @Param("batchDate") String batchDate,
            @Param("batchExecuteSeq") int batchExecuteSeq,
            @Param("resRtCode") String resRtCode,
            @Param("batchEndDtime") String batchEndDtime,
            @Param("errorReason") String errorReason,
            @Param("executeCount") long executeCount,
            @Param("successCount") long successCount,
            @Param("failCount") long failCount,
            @Param("lastUpdateUserId") String lastUpdateUserId);
}
