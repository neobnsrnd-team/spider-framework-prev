package com.example.spiderbatch.spi;

/**
 * @file BatchHistoryRecorder.java
 * @description FWK_BATCH_HIS 이력 기록을 플러그인 방식으로 교체 가능하게 분리한 SPI 인터페이스.
 *
 * <p>기본 구현체({@code DefaultBatchHistoryRecorder})는 Oracle MyBatis 매퍼를 사용한다.
 * 소비자 프로젝트가 다른 저장소(다른 DB, 메시지 큐 등)를 사용하려면
 * 이 인터페이스를 구현한 Bean을 등록하면 자동으로 기본 구현체를 대체한다.</p>
 *
 * @example
 * <pre>{@code
 * @Bean
 * public BatchHistoryRecorder customRecorder() {
 *     return new MyCustomBatchHistoryRecorder();
 * }
 * }</pre>
 */
public interface BatchHistoryRecorder {

    /**
     * 같은 날, 같은 배치, 같은 인스턴스의 다음 실행 회차를 계산한다.
     * NVL(MAX(BATCH_EXECUTE_SEQ), 0) + 1 패턴.
     *
     * @param batchAppId 배치 APP ID
     * @param instanceId WAS 인스턴스 ID
     * @param batchDate  배치 기준일 (YYYYMMDD)
     * @return 다음 실행 회차 (첫 실행이면 1)
     */
    int nextExecuteSeq(String batchAppId, String instanceId, String batchDate);

    /**
     * 배치 실행 시작 이력을 INSERT한다 (RES_RT_CODE = '0').
     *
     * @param batchAppId       배치 APP ID
     * @param instanceId       WAS 인스턴스 ID
     * @param batchDate        배치 기준일 (YYYYMMDD)
     * @param batchExecuteSeq  실행 회차
     * @param logDtime         시작 일시 (yyyyMMddHHmmssSSS)
     * @param userId           실행자 ID
     */
    void insertStarted(String batchAppId, String instanceId, String batchDate,
                       int batchExecuteSeq, String logDtime, String userId);

    /**
     * 배치 실행 결과를 UPDATE한다 (SUCCESS: '1', ABNORMAL: '9').
     *
     * @param batchAppId       배치 APP ID
     * @param instanceId       WAS 인스턴스 ID
     * @param batchDate        배치 기준일
     * @param batchExecuteSeq  실행 회차
     * @param resRtCode        결과 코드
     * @param batchEndDtime    종료 일시
     * @param errorReason      오류 사유 (정상 종료 시 null)
     * @param executeCount     읽은 건수
     * @param successCount     쓴 건수
     * @param failCount        스킵 건수
     * @param userId           실행자 ID
     * @return UPDATE 성공 건수 (0이면 PK 불일치 경고)
     */
    int updateResult(String batchAppId, String instanceId, String batchDate,
                     int batchExecuteSeq, String resRtCode, String batchEndDtime,
                     String errorReason, long executeCount, long successCount,
                     long failCount, String userId);
}
