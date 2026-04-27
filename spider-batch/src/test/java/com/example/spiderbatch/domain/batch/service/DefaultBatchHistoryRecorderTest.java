package com.example.spiderbatch.domain.batch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.spiderbatch.domain.batch.mapper.BatchHisMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("DefaultBatchHistoryRecorder 테스트")
@ExtendWith(MockitoExtension.class)
class DefaultBatchHistoryRecorderTest {

    @Mock
    private BatchHisMapper batchHisMapper;

    @InjectMocks
    private DefaultBatchHistoryRecorder recorder;

    @Test
    @DisplayName("nextExecuteSeq — BatchHisMapper.selectNextExecuteSeq에 위임한다")
    void nextExecuteSeq_delegatesToMapper() {
        when(batchHisMapper.selectNextExecuteSeq("APP01", "WAS01", "20240101")).thenReturn(3);

        int result = recorder.nextExecuteSeq("APP01", "WAS01", "20240101");

        assertThat(result).isEqualTo(3);
        verify(batchHisMapper).selectNextExecuteSeq("APP01", "WAS01", "20240101");
    }

    @Test
    @DisplayName("nextExecuteSeq — 최초 실행(seq=1)을 정상 반환한다")
    void nextExecuteSeq_firstExecution_returns1() {
        when(batchHisMapper.selectNextExecuteSeq("APP01", "WAS01", "20240101")).thenReturn(1);

        assertThat(recorder.nextExecuteSeq("APP01", "WAS01", "20240101")).isEqualTo(1);
    }

    @Test
    @DisplayName("insertStarted — BatchHisMapper.insertBatchHis에 위임한다")
    void insertStarted_delegatesToMapper() {
        recorder.insertStarted("APP01", "WAS01", "20240101", 1, "20240101100000000", "SYSTEM");

        verify(batchHisMapper).insertBatchHis(
                "APP01", "WAS01", "20240101", 1, "20240101100000000", "SYSTEM");
    }

    @Test
    @DisplayName("updateResult — BatchHisMapper.updateBatchHisResult에 위임하고 반환값을 그대로 전달한다")
    void updateResult_delegatesToMapperAndReturnsCount() {
        when(batchHisMapper.updateBatchHisResult(
                "APP01", "WAS01", "20240101", 1,
                "1", "20240101101000000", null,
                100L, 100L, 0L, "SYSTEM"))
                .thenReturn(1);

        int result = recorder.updateResult(
                "APP01", "WAS01", "20240101", 1,
                "1", "20240101101000000", null,
                100L, 100L, 0L, "SYSTEM");

        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("updateResult — PK 불일치 시 0을 반환한다")
    void updateResult_pkMismatch_returns0() {
        when(batchHisMapper.updateBatchHisResult(
                "APP01", "WAS01", "20240101", 99,
                "9", "20240101101000000", "오류",
                0L, 0L, 0L, "SYSTEM"))
                .thenReturn(0);

        int result = recorder.updateResult(
                "APP01", "WAS01", "20240101", 99,
                "9", "20240101101000000", "오류",
                0L, 0L, 0L, "SYSTEM");

        assertThat(result).isEqualTo(0);
    }
}
