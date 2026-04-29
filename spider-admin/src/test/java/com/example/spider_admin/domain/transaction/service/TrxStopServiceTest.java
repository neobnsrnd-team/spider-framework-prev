package com.example.spider_admin.domain.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.spider_admin.domain.transaction.dto.OperModeBatchRequest;
import com.example.spider_admin.domain.transaction.dto.TrxSimpleResponse;
import com.example.spider_admin.domain.transaction.dto.TrxStopBatchRequest;
import com.example.spider_admin.domain.transaction.dto.TrxStopListResponse;
import com.example.spider_admin.domain.transaction.dto.TrxStopSearchRequest;
import com.example.spider_admin.domain.transaction.mapper.TrxMapper;
import com.example.spider_admin.domain.transaction.mapper.TrxStopHistoryMapper;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.exception.InvalidInputException;
import com.example.spider_admin.global.exception.NotFoundException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrxStopService 테스트")
class TrxStopServiceTest {

    @Mock
    private TrxMapper trxMapper;

    @Mock
    private TrxStopHistoryMapper trxStopHistoryMapper;

    @InjectMocks
    private TrxStopService trxStopService;

    // ─── searchTrxStopList ────────────────────────────────────────────

    @Test
    @DisplayName("[조회] 검색 결과를 PageResponse로 반환해야 한다")
    void searchTrxStopList_returnsPageResponse() {
        PageRequest pageRequest = PageRequest.builder().page(0).size(20).build();
        TrxStopSearchRequest searchDTO = TrxStopSearchRequest.builder().build();

        List<TrxStopListResponse> data = List.of(buildListResponse("E2E-TRX-001"), buildListResponse("E2E-TRX-002"));
        given(trxMapper.countAllForTrxStop(any(), any(), any(), any())).willReturn(2L);
        given(trxMapper.findAllForTrxStop(
                        any(), any(), any(), any(), any(), any(), any(Integer.class), any(Integer.class)))
                .willReturn(data);

        PageResponse<TrxStopListResponse> result = trxStopService.searchTrxStopList(pageRequest, searchDTO);

        assertThat(result.getTotalElements()).isEqualTo(2L);
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("[조회] 검색 결과가 없으면 빈 content를 반환해야 한다")
    void searchTrxStopList_noResult_returnsEmptyContent() {
        PageRequest pageRequest = PageRequest.builder().page(0).size(20).build();
        TrxStopSearchRequest searchDTO = TrxStopSearchRequest.builder().build();

        given(trxMapper.countAllForTrxStop(any(), any(), any(), any())).willReturn(0L);
        given(trxMapper.findAllForTrxStop(
                        any(), any(), any(), any(), any(), any(), any(Integer.class), any(Integer.class)))
                .willReturn(List.of());

        PageResponse<TrxStopListResponse> result = trxStopService.searchTrxStopList(pageRequest, searchDTO);

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    // ─── batchUpdateTrxStop ───────────────────────────────────────────

    @Test
    @DisplayName("[일괄중지] 상태 변경이 필요한 거래가 있으면 일괄 업데이트를 수행해야 한다")
    void batchUpdateTrxStop_withChange_callsBatchUpdate() {
        TrxStopBatchRequest request = TrxStopBatchRequest.builder()
                .trxIds(List.of("E2E-TRX-001", "E2E-TRX-002"))
                .trxStopYn("Y")
                .trxStopReason("테스트 중지")
                .build();

        given(trxMapper.selectSimpleByIds(anyList()))
                .willReturn(List.of(buildSimpleResponse("E2E-TRX-001", "N"), buildSimpleResponse("E2E-TRX-002", "N")));

        trxStopService.batchUpdateTrxStop(request);

        then(trxMapper).should().batchUpdateTrxStop(anyList(), anyString(), any());
        then(trxStopHistoryMapper).should().insertBatch(anyList());
    }

    @Test
    @DisplayName("[일괄중지] 이미 동일한 상태이면 업데이트를 수행하지 않아야 한다")
    void batchUpdateTrxStop_alreadySameState_skipsUpdate() {
        TrxStopBatchRequest request = TrxStopBatchRequest.builder()
                .trxIds(List.of("E2E-TRX-001"))
                .trxStopYn("Y")
                .build();

        given(trxMapper.selectSimpleByIds(anyList())).willReturn(List.of(buildSimpleResponse("E2E-TRX-001", "Y")));

        trxStopService.batchUpdateTrxStop(request);

        then(trxMapper).shouldHaveNoMoreInteractions();
        then(trxStopHistoryMapper).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("[일괄중지] 존재하지 않는 거래ID가 포함된 경우 NotFoundException을 발생시켜야 한다")
    void batchUpdateTrxStop_missingTrxId_throwsNotFoundException() {
        TrxStopBatchRequest request = TrxStopBatchRequest.builder()
                .trxIds(List.of("E2E-TRX-001", "NOT-EXIST"))
                .trxStopYn("Y")
                .build();

        given(trxMapper.selectSimpleByIds(anyList())).willReturn(List.of(buildSimpleResponse("E2E-TRX-001", "N")));

        assertThatThrownBy(() -> trxStopService.batchUpdateTrxStop(request)).isInstanceOf(NotFoundException.class);
    }

    // ─── batchUpdateOperMode ──────────────────────────────────────────

    @Test
    @DisplayName("[운영모드] 유효한 코드(D/R/T)이면 전체 업데이트를 수행해야 한다")
    void batchUpdateOperMode_validCode_callsUpdateAll() {
        OperModeBatchRequest request =
                OperModeBatchRequest.builder().operModeType("D").build();

        trxStopService.batchUpdateOperMode(request);

        then(trxMapper).should().updateAllOperMode("D");
    }

    @Test
    @DisplayName("[운영모드] null이면 전체 초기화를 수행해야 한다")
    void batchUpdateOperMode_null_callsUpdateAll() {
        OperModeBatchRequest request =
                OperModeBatchRequest.builder().operModeType(null).build();

        trxStopService.batchUpdateOperMode(request);

        then(trxMapper).should().updateAllOperMode(null);
    }

    @Test
    @DisplayName("[운영모드] 유효하지 않은 코드이면 InvalidInputException을 발생시켜야 한다")
    void batchUpdateOperMode_invalidCode_throwsInvalidInputException() {
        OperModeBatchRequest request =
                OperModeBatchRequest.builder().operModeType("X").build();

        assertThatThrownBy(() -> trxStopService.batchUpdateOperMode(request)).isInstanceOf(InvalidInputException.class);
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────

    private TrxStopListResponse buildListResponse(String trxId) {
        return TrxStopListResponse.builder()
                .trxId(trxId)
                .trxName("E2E 거래명")
                .operModeType("O")
                .trxType("A")
                .retryTrxYn("N")
                .trxStopYn("N")
                .accessUserCount(0)
                .build();
    }

    private TrxSimpleResponse buildSimpleResponse(String trxId, String trxStopYn) {
        return TrxSimpleResponse.builder()
                .trxId(trxId)
                .trxName("E2E 거래명")
                .trxStopYn(trxStopYn)
                .build();
    }
}
