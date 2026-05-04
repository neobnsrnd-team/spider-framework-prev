package com.example.spideradmin.domain.adminhistory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;

import com.example.spideradmin.domain.adminhistory.dto.AdminActionLogResponse;
import com.example.spideradmin.domain.adminhistory.dto.AdminActionLogSearchRequest;
import com.example.spideradmin.domain.adminhistory.mapper.AdminActionLogMapper;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.exception.InvalidInputException;
import com.example.spideradmin.global.util.ExcelExportUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminActionLogService 테스트")
class AdminActionLogServiceTest {

    @Mock
    private AdminActionLogMapper adminActionLogMapper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AdminActionLogService adminActionLogService;

    // ─── searchLogs ───────────────────────────────────────────────────

    @Test
    @DisplayName("[조회] 검색 조건이 있으면 DB를 조회하고 PageResponse를 반환한다")
    void searchLogs_withCondition_returnsPageResponse() {
        AdminActionLogSearchRequest search =
                AdminActionLogSearchRequest.builder().userId("e2e-admin").build();
        PageRequest pageRequest = PageRequest.builder().page(0).size(10).build();

        List<AdminActionLogResponse> data = List.of(buildResponse(), buildResponse());
        given(adminActionLogMapper.countSearchLogs(search)).willReturn(2L);
        given(adminActionLogMapper.searchLogs(any(), anyInt(), anyInt())).willReturn(data);

        PageResponse<AdminActionLogResponse> result = adminActionLogService.searchLogs(search, pageRequest);

        assertThat(result.getTotalElements()).isEqualTo(2L);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getUserId()).isEqualTo("e2e-admin");
    }

    @Test
    @DisplayName("[조회] 검색 조건이 없어도 전체 데이터를 조회한다")
    void searchLogs_noCondition_returnsAll() {
        AdminActionLogSearchRequest search =
                AdminActionLogSearchRequest.builder().build();
        PageRequest pageRequest = PageRequest.builder().page(0).size(10).build();

        given(adminActionLogMapper.countSearchLogs(search)).willReturn(5L);
        given(adminActionLogMapper.searchLogs(any(), anyInt(), anyInt())).willReturn(List.of(buildResponse()));

        PageResponse<AdminActionLogResponse> result = adminActionLogService.searchLogs(search, pageRequest);

        assertThat(result.getTotalElements()).isEqualTo(5L);
        assertThat(result.getContent()).isNotEmpty();
    }

    @Test
    @DisplayName("[조회] accessIp 조건만 있어도 DB를 조회한다")
    void searchLogs_withAccessIp_queriesDb() {
        AdminActionLogSearchRequest search =
                AdminActionLogSearchRequest.builder().accessIp("127.0.0.1").build();
        PageRequest pageRequest = PageRequest.builder().page(0).size(10).build();

        given(adminActionLogMapper.countSearchLogs(search)).willReturn(1L);
        given(adminActionLogMapper.searchLogs(any(), anyInt(), anyInt())).willReturn(List.of(buildResponse()));

        assertThat(adminActionLogService.searchLogs(search, pageRequest).getTotalElements())
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("[조회] accessUrl 조건만 있어도 DB를 조회한다")
    void searchLogs_withAccessUrl_queriesDb() {
        AdminActionLogSearchRequest search =
                AdminActionLogSearchRequest.builder().accessUrl("/api/users").build();
        PageRequest pageRequest = PageRequest.builder().page(0).size(10).build();

        given(adminActionLogMapper.countSearchLogs(search)).willReturn(1L);
        given(adminActionLogMapper.searchLogs(any(), anyInt(), anyInt())).willReturn(List.of(buildResponse()));

        assertThat(adminActionLogService.searchLogs(search, pageRequest).getTotalElements())
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("[조회] startDate 조건만 있어도 DB를 조회한다")
    void searchLogs_withStartDate_queriesDb() {
        AdminActionLogSearchRequest search = AdminActionLogSearchRequest.builder()
                .startDate("20260101000000")
                .build();
        PageRequest pageRequest = PageRequest.builder().page(0).size(10).build();

        given(adminActionLogMapper.countSearchLogs(search)).willReturn(1L);
        given(adminActionLogMapper.searchLogs(any(), anyInt(), anyInt())).willReturn(List.of(buildResponse()));

        assertThat(adminActionLogService.searchLogs(search, pageRequest).getTotalElements())
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("[조회] 검색 결과가 0건이면 빈 content를 반환한다")
    void searchLogs_noResult_returnsEmptyContent() {
        AdminActionLogSearchRequest search =
                AdminActionLogSearchRequest.builder().userId("NON_EXIST").build();
        PageRequest pageRequest = PageRequest.builder().page(0).size(10).build();

        given(adminActionLogMapper.countSearchLogs(search)).willReturn(0L);
        given(adminActionLogMapper.searchLogs(any(), anyInt(), anyInt())).willReturn(List.of());

        PageResponse<AdminActionLogResponse> result = adminActionLogService.searchLogs(search, pageRequest);

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    // ─── exportLogs ───────────────────────────────────────────────────

    @Test
    @DisplayName("[엑셀] 데이터가 있으면 xlsx 바이트 배열을 반환한다")
    void exportLogs_withData_returnsBytes() {
        AdminActionLogSearchRequest search =
                AdminActionLogSearchRequest.builder().userId("e2e-admin").build();

        given(adminActionLogMapper.findAllForExport(search)).willReturn(List.of(buildResponse(), buildResponse()));

        byte[] result = adminActionLogService.exportLogs(search);

        assertThat(result).isNotNull().hasSizeGreaterThan(0);
    }

    @Test
    @DisplayName("[엑셀] 데이터가 없어도 빈 xlsx 바이트 배열을 반환한다")
    void exportLogs_emptyData_returnsBytes() {
        AdminActionLogSearchRequest search =
                AdminActionLogSearchRequest.builder().build();
        given(adminActionLogMapper.findAllForExport(search)).willReturn(List.of());

        byte[] result = adminActionLogService.exportLogs(search);

        assertThat(result).isNotNull().hasSizeGreaterThan(0);
    }

    @Test
    @DisplayName("[엑셀] 행 수가 한도를 초과하면 InvalidInputException을 던진다")
    void exportLogs_exceedsLimit_throwsInvalidInputException() {
        AdminActionLogSearchRequest search =
                AdminActionLogSearchRequest.builder().userId("e2e-admin").build();

        List<AdminActionLogResponse> overLimit =
                Collections.nCopies(ExcelExportUtil.MAX_ROW_LIMIT + 1, buildResponse());
        given(adminActionLogMapper.findAllForExport(search)).willReturn(overLimit);

        assertThatThrownBy(() -> adminActionLogService.exportLogs(search)).isInstanceOf(InvalidInputException.class);
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────

    private AdminActionLogResponse buildResponse() {
        return AdminActionLogResponse.builder()
                .userId("e2e-admin")
                .accessDtime("20260312120000")
                .accessIp("127.0.0.1")
                .accessUrl("[GET] /api/users")
                .inputData("{\"traceId\":\"abc-123\",\"phase\":\"RES\"}")
                .resultMessage("정상처리")
                .build();
    }
}
