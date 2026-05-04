package com.example.spiderbatch.domain.batch.service;

import com.example.spiderbatch.domain.batch.mapper.BatchHisMapper;
import com.example.spiderbatch.spi.BatchHistoryRecorder;
import lombok.RequiredArgsConstructor;

/**
 * {@link BatchHistoryRecorder}의 기본 구현체.
 *
 * <p>Oracle MyBatis 매퍼({@link BatchHisMapper})를 사용하여 FWK_BATCH_HIS에 이력을 기록한다.
 * 내장 프로젝트에서 별도 {@link BatchHistoryRecorder} Bean을 등록하지 않으면
 * {@link com.example.spiderbatch.config.SpiderBatchAutoConfiguration}이 이 Bean을 자동 등록한다.</p>
 */
@RequiredArgsConstructor
public class DefaultBatchHistoryRecorder implements BatchHistoryRecorder {

    private final BatchHisMapper batchHisMapper;

    @Override
    public int nextExecuteSeq(String batchAppId, String instanceId, String batchDate) {
        return batchHisMapper.selectNextExecuteSeq(batchAppId, instanceId, batchDate);
    }

    @Override
    public void insertStarted(String batchAppId, String instanceId, String batchDate,
                              int batchExecuteSeq, String logDtime, String userId) {
        batchHisMapper.insertBatchHis(batchAppId, instanceId, batchDate, batchExecuteSeq, logDtime, userId);
    }

    @Override
    public int updateResult(String batchAppId, String instanceId, String batchDate,
                            int batchExecuteSeq, String resRtCode, String batchEndDtime,
                            String errorReason, long executeCount, long successCount,
                            long failCount, String userId) {
        return batchHisMapper.updateBatchHisResult(
                batchAppId, instanceId, batchDate, batchExecuteSeq, resRtCode, batchEndDtime,
                errorReason, executeCount, successCount, failCount, userId);
    }
}
