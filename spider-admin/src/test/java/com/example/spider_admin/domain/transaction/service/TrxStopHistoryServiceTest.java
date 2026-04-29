package com.example.spider_admin.domain.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;

import com.example.spider_admin.domain.transaction.dto.TrxStopHistorySearchRequest;
import com.example.spider_admin.domain.transaction.dto.TrxStopHistoryWithTrxNameResponse;
import com.example.spider_admin.domain.transaction.mapper.TrxStopHistoryMapper;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.exception.InvalidInputException;
import com.example.spider_admin.global.util.ExcelExportUtil;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrxStopHistoryService 테스트")
class TrxStopHistoryServiceTest {

    @Mock
    private TrxStopHistoryMapper trxStopHistoryMapper;

    @InjectMocks
    private TrxStopHistoryService trxStopHistoryService;

    // ─── searchHistories ──────────────────────────────────────────────

    @Test
    @DisplayName("[조회] 데이터가 있으면 count=2인 PageResponse를 반환한다")
    void searchHistories_withData_returnsPageResponse() {
        TrxStopHistorySearchRequest search =
                TrxStopHistorySearchRequest.builder().gubunType("T").build();
        PageRequest pageRequest = PageRequest.builder().page(0).size(10).build();

        List<TrxStopHistoryWithTrxNameResponse> data =
                List.of(buildResponse("E2E-TRX-001"), buildResponse("E2E-TRX-002"));
        given(trxStopHistoryMapper.countSearchHistories(search)).willReturn(2L);
        given(trxStopHistoryMapper.searchHistories(
                        any(), nullable(String.class), nullable(String.class), anyInt(), anyInt()))
                .willReturn(data);

        PageResponse<TrxStopHistoryWithTrxNameResponse> result =
                trxStopHistoryService.searchHistories(search, pageRequest);

        assertThat(result.getTotalElements()).isEqualTo(2L);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTrxId()).isEqualTo("E2E-TRX-001");
    }

    @Test
    @DisplayName("[조회] 검색 결과가 없으면 빈 content를 반환한다")
    void searchHistories_emptyResult_returnsEmptyContent() {
        TrxStopHistorySearchRequest search =
                TrxStopHistorySearchRequest.builder().trxId("NON_EXIST").build();
        PageRequest pageRequest = PageRequest.builder().page(0).size(10).build();

        given(trxStopHistoryMapper.countSearchHistories(search)).willReturn(0L);
        given(trxStopHistoryMapper.searchHistories(
                        any(), nullable(String.class), nullable(String.class), anyInt(), anyInt()))
                .willReturn(List.of());

        PageResponse<TrxStopHistoryWithTrxNameResponse> result =
                trxStopHistoryService.searchHistories(search, pageRequest);

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    // ─── findByTrxId ─────────────────────────────────────────────────

    @Test
    @DisplayName("[거래ID 조회] 데이터가 있으면 mapper 반환값을 그대로 반환한다")
    void findByTrxId_withData_returnsList() {
        List<TrxStopHistoryWithTrxNameResponse> data =
                List.of(buildResponse("E2E-TRX-001"), buildResponse("E2E-TRX-001"));
        given(trxStopHistoryMapper.findByTrxId("E2E-TRX-001")).willReturn(data);

        List<TrxStopHistoryWithTrxNameResponse> result = trxStopHistoryService.findByTrxId("E2E-TRX-001");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTrxId()).isEqualTo("E2E-TRX-001");
    }

    @Test
    @DisplayName("[거래ID 조회] 데이터가 없으면 빈 리스트를 반환한다")
    void findByTrxId_noData_returnsEmptyList() {
        given(trxStopHistoryMapper.findByTrxId("NO-SUCH-ID")).willReturn(List.of());

        List<TrxStopHistoryWithTrxNameResponse> result = trxStopHistoryService.findByTrxId("NO-SUCH-ID");

        assertThat(result).isEmpty();
    }

    // ─── exportExcel ─────────────────────────────────────────────────

    @Test
    @DisplayName("[엑셀] 데이터가 있으면 xlsx 바이트 배열을 반환한다")
    void exportExcel_withData_returnsBytes() {
        TrxStopHistorySearchRequest search =
                TrxStopHistorySearchRequest.builder().gubunType("T").build();

        given(trxStopHistoryMapper.findAllForExport(search))
                .willReturn(List.of(buildResponse("E2E-TRX-001"), buildResponse("E2E-TRX-002")));

        byte[] result = trxStopHistoryService.exportExcel(search);

        assertThat(result).isNotNull().hasSizeGreaterThan(0);
    }

    @Test
    @DisplayName("[엑셀] 데이터가 없어도 빈 xlsx 바이트 배열을 반환한다")
    void exportExcel_emptyData_returnsBytes() {
        TrxStopHistorySearchRequest search =
                TrxStopHistorySearchRequest.builder().build();
        given(trxStopHistoryMapper.findAllForExport(search)).willReturn(List.of());

        byte[] result = trxStopHistoryService.exportExcel(search);

        assertThat(result).isNotNull().hasSizeGreaterThan(0);
    }

    @Test
    @DisplayName("[엑셀] 행 수가 한도를 초과하면 InvalidInputException을 던진다")
    void exportExcel_exceedsLimit_throwsInvalidInputException() {
        TrxStopHistorySearchRequest search =
                TrxStopHistorySearchRequest.builder().gubunType("T").build();

        List<TrxStopHistoryWithTrxNameResponse> overLimit =
                Collections.nCopies(ExcelExportUtil.MAX_ROW_LIMIT + 1, buildResponse("E2E-TRX-001"));
        given(trxStopHistoryMapper.findAllForExport(search)).willReturn(overLimit);

        assertThatThrownBy(() -> trxStopHistoryService.exportExcel(search)).isInstanceOf(InvalidInputException.class);
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────

    private TrxStopHistoryWithTrxNameResponse buildResponse(String trxId) {
        return TrxStopHistoryWithTrxNameResponse.builder()
                .gubunType("T")
                .trxId(trxId)
                .trxName("거래명")
                .trxStopUpdateDtime("20260301090000")
                .trxStopReason("테스트")
                .trxStopYn("Y")
                .lastUpdateUserId("e2e-admin")
                .build();
    }
}
