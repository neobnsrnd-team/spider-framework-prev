package com.example.spider_admin.domain.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.spider_admin.domain.transaction.dto.TrxCreateRequest;
import com.example.spider_admin.domain.transaction.dto.TrxCreateResponse;
import com.example.spider_admin.domain.transaction.dto.TrxDetailResponse;
import com.example.spider_admin.domain.transaction.dto.TrxResponse;
import com.example.spider_admin.domain.transaction.dto.TrxUpdateRequest;
import com.example.spider_admin.domain.transaction.dto.TrxWithMessagesResponse;
import com.example.spider_admin.domain.transaction.mapper.TrxHistoryMapper;
import com.example.spider_admin.domain.transaction.mapper.TrxMapper;
import com.example.spider_admin.global.exception.DuplicateException;
import com.example.spider_admin.global.exception.InternalException;
import com.example.spider_admin.global.exception.NotFoundException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrxService 테스트")
class TrxServiceTest {

    @Mock
    private TrxMapper trxMapper;

    @Mock
    private TrxHistoryMapper trxHistoryMapper;

    @InjectMocks
    private TrxService trxService;

    // ─── getTrxById ───────────────────────────────────────────────────

    @Test
    @DisplayName("[조회] 존재하는 ID이면 TrxResponse를 반환해야 한다")
    void getTrxById_exists_returnsTrxResponse() {
        given(trxMapper.selectResponseById("E2E-TRX-001")).willReturn(buildResponse("E2E-TRX-001"));

        TrxResponse result = trxService.getTrxById("E2E-TRX-001");

        assertThat(result.getTrxId()).isEqualTo("E2E-TRX-001");
    }

    @Test
    @DisplayName("[조회] 존재하지 않는 ID이면 NotFoundException을 발생시켜야 한다")
    void getTrxById_notExists_throwsNotFoundException() {
        given(trxMapper.selectResponseById("NOT-EXIST")).willReturn(null);

        assertThatThrownBy(() -> trxService.getTrxById("NOT-EXIST")).isInstanceOf(NotFoundException.class);
    }

    // ─── updateTrx ────────────────────────────────────────────────────

    @Test
    @DisplayName("[수정] 존재하는 ID 수정 시 selectResponseById 결과를 반환해야 한다")
    void updateTrx_exists_returnsTrxResponse() {
        TrxUpdateRequest dto = buildUpdateRequest();
        given(trxMapper.updateTrx("E2E-TRX-001", dto)).willReturn(1);
        given(trxMapper.selectResponseById("E2E-TRX-001")).willReturn(buildResponse("E2E-TRX-001"));

        TrxResponse result = trxService.updateTrx("E2E-TRX-001", dto);

        assertThat(result.getTrxId()).isEqualTo("E2E-TRX-001");
        then(trxMapper).should().updateTrx("E2E-TRX-001", dto);
    }

    @Test
    @DisplayName("[수정] 존재하지 않는 ID 수정 시 NotFoundException을 발생시켜야 한다")
    void updateTrx_notExists_throwsNotFoundException() {
        TrxUpdateRequest dto = buildUpdateRequest();
        given(trxMapper.updateTrx("NOT-EXIST", dto)).willReturn(0);

        assertThatThrownBy(() -> trxService.updateTrx("NOT-EXIST", dto)).isInstanceOf(NotFoundException.class);
    }

    // ─── createTrx ────────────────────────────────────────────────────

    @Test
    @DisplayName("[생성] 중복되지 않는 ID이면 TrxCreateResponse를 반환해야 한다")
    void createTrx_success_returnsTrxCreateResponse() {
        TrxCreateRequest req = buildCreateRequest("NEW-TRX-001");
        given(trxMapper.countByTrxId("NEW-TRX-001")).willReturn(0);
        given(trxMapper.selectResponseById("NEW-TRX-001")).willReturn(buildResponse("NEW-TRX-001"));

        TrxCreateResponse result = trxService.createTrx(req);

        assertThat(result.getTrxId()).isEqualTo("NEW-TRX-001");
        then(trxMapper).should().insertTrx(req);
    }

    @Test
    @DisplayName("[생성] 중복 ID이면 DuplicateException을 발생시켜야 한다")
    void createTrx_duplicateId_throwsDuplicateException() {
        TrxCreateRequest req = buildCreateRequest("DUP-TRX-001");
        given(trxMapper.countByTrxId("DUP-TRX-001")).willReturn(1);

        assertThatThrownBy(() -> trxService.createTrx(req)).isInstanceOf(DuplicateException.class);
    }

    @Test
    @DisplayName("[생성] insert 후 조회 결과가 null이면 InternalException을 발생시켜야 한다")
    void createTrx_insertThenNullSelect_throwsInternalException() {
        TrxCreateRequest req = buildCreateRequest("NULL-TRX-001");
        given(trxMapper.countByTrxId("NULL-TRX-001")).willReturn(0);
        given(trxMapper.selectResponseById("NULL-TRX-001")).willReturn(null);

        assertThatThrownBy(() -> trxService.createTrx(req)).isInstanceOf(InternalException.class);
    }

    // ─── deleteTrx ────────────────────────────────────────────────────

    @Test
    @DisplayName("[삭제] 존재하는 ID이면 히스토리 생성 후 삭제되어야 한다")
    void deleteTrx_exists_deletesWithHistory() {
        given(trxMapper.countByTrxId("E2E-TRX-001")).willReturn(1);
        given(trxHistoryMapper.getNextVersion("E2E-TRX-001")).willReturn(1);

        trxService.deleteTrx("E2E-TRX-001");

        then(trxHistoryMapper).should().insertHistoryFromTrx("E2E-TRX-001", 1, "Delete operation");
        then(trxMapper).should().deleteTrxById("E2E-TRX-001");
    }

    @Test
    @DisplayName("[삭제] 존재하지 않는 ID이면 NotFoundException을 발생시켜야 한다")
    void deleteTrx_notExists_throwsNotFoundException() {
        given(trxMapper.countByTrxId("NOT-EXIST")).willReturn(0);

        assertThatThrownBy(() -> trxService.deleteTrx("NOT-EXIST")).isInstanceOf(NotFoundException.class);
    }

    // ─── getTrxDetail ─────────────────────────────────────────────────

    @Test
    @DisplayName("[상세조회] 존재하는 ID이면 TrxDetailResponse를 반환해야 한다")
    void getTrxDetail_exists_returnsTrxDetailResponse() {
        given(trxMapper.findTrxDetailById("E2E-TRX-001")).willReturn(buildDetailResponse("E2E-TRX-001"));

        TrxDetailResponse result = trxService.getTrxDetail("E2E-TRX-001");

        assertThat(result.getTrxId()).isEqualTo("E2E-TRX-001");
    }

    @Test
    @DisplayName("[상세조회] 존재하지 않는 ID이면 NotFoundException을 발생시켜야 한다")
    void getTrxDetail_notExists_throwsNotFoundException() {
        given(trxMapper.findTrxDetailById("NOT-EXIST")).willReturn(null);

        assertThatThrownBy(() -> trxService.getTrxDetail("NOT-EXIST")).isInstanceOf(NotFoundException.class);
    }

    // ─── getTrxWithMessages ───────────────────────────────────────────

    @Test
    @DisplayName("[전문포함조회] 존재하는 ID이면 TrxWithMessagesResponse를 반환해야 한다")
    void getTrxWithMessages_exists_returnsTrxWithMessagesResponse() {
        given(trxMapper.findTrxDetailById("E2E-TRX-001")).willReturn(buildDetailResponse("E2E-TRX-001"));
        given(trxMapper.findMessagesByTrxId("E2E-TRX-001")).willReturn(List.of());

        TrxWithMessagesResponse result = trxService.getTrxWithMessages("E2E-TRX-001");

        assertThat(result.getTrxId()).isEqualTo("E2E-TRX-001");
        assertThat(result.getMessages()).isEmpty();
    }

    @Test
    @DisplayName("[전문포함조회] 존재하지 않는 ID이면 NotFoundException을 발생시켜야 한다")
    void getTrxWithMessages_notExists_throwsNotFoundException() {
        given(trxMapper.findTrxDetailById("NOT-EXIST")).willReturn(null);

        assertThatThrownBy(() -> trxService.getTrxWithMessages("NOT-EXIST")).isInstanceOf(NotFoundException.class);
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────

    private TrxResponse buildResponse(String trxId) {
        return TrxResponse.builder()
                .trxId(trxId)
                .trxName("E2E 거래명")
                .trxType("A")
                .retryTrxYn("N")
                .maxRetryCount(0)
                .trxStopYn("N")
                .operModeType("O")
                .build();
    }

    private TrxCreateRequest buildCreateRequest(String trxId) {
        return TrxCreateRequest.builder()
                .trxId(trxId)
                .trxType("1")
                .retryTrxYn("N")
                .maxRetryCount(0)
                .build();
    }

    private TrxDetailResponse buildDetailResponse(String trxId) {
        return TrxDetailResponse.builder()
                .trxId(trxId)
                .trxName("E2E 거래명")
                .trxType("1")
                .retryTrxYn("N")
                .maxRetryCount(0)
                .trxStopYn("N")
                .operModeType("O")
                .build();
    }

    private TrxUpdateRequest buildUpdateRequest() {
        return TrxUpdateRequest.builder()
                .trxName("수정된 거래명")
                .trxType("A")
                .retryTrxYn("N")
                .maxRetryCount(3)
                .build();
    }
}
